#!/usr/bin/env python3
"""
Generate an editable UML class diagram from a Java Maven project.

Requirements:
    pip install tree-sitter-language-pack

Optional:
    Install Graphviz (the `dot` executable) for automatic graph layout.
    Install diagrams.net Desktop for opening/editing and PNG export.

Usage:
    python java_uml.py "C:/path/to/maven-project" --open
    python java_uml.py "/path/to/project" --output "./uml" --open --png
"""

from __future__ import annotations

import argparse
import html
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

try:
    from tree_sitter_language_pack import get_parser
except ImportError:
    print("Missing dependency. Install it with: pip install tree-sitter-language-pack")
    raise


@dataclass
class UMLClass:
    name: str
    qualified_name: str
    kind: str
    fields: list[str] = field(default_factory=list)
    methods: list[str] = field(default_factory=list)
    bases: list[tuple[str, str]] = field(default_factory=list)
    field_types: set[str] = field(default_factory=set)


@dataclass
class Relationship:
    source: str
    target: str
    kind: str  # inheritance, realization, association


def text_of(node, source: bytes) -> str:
    if node is None:
        return ""
    return source[node.start_byte:node.end_byte].decode("utf-8", errors="replace")


def clean_type(value: str) -> str:
    value = re.sub(r"@\w+(?:\([^)]*\))?\s*", "", value)
    value = re.sub(r"\s+", "", value)
    return value.strip()


def simple_type_names(type_text: str) -> set[str]:
    """Extract plausible class names from types, including generic arguments."""
    # Remove arrays, wildcards and primitive types; retain identifiers.
    ignored = {
        "byte", "short", "int", "long", "float", "double", "boolean",
        "char", "void", "extends", "super"
    }
    names = set(re.findall(r"[A-Za-z_$][\w$]*", type_text))
    return {n for n in names if n not in ignored}


def node_children(node, source: bytes):
    return [(child, text_of(child, source)) for child in node.named_children]


def find_descendant(node, wanted: set[str]):
    stack = [node]
    while stack:
        current = stack.pop()
        if current.type in wanted:
            return current
        stack.extend(reversed(current.named_children))
    return None


def modifiers_text(node, source: bytes) -> str:
    mods = node.child_by_field_name("modifiers")
    return text_of(mods, source) if mods else ""


def visibility(modifiers: str) -> str:
    if "public" in modifiers:
        return "+"
    if "protected" in modifiers:
        return "#"
    if "private" in modifiers:
        return "-"
    return "~"


def parse_java_file(path: Path, parser) -> list[UMLClass]:
    source = path.read_bytes()
    tree = parser.parse(source)
    result: list[UMLClass] = []

    package = ""
    package_node = find_descendant(tree.root_node, {"package_declaration"})
    if package_node:
        package = text_of(package_node, source).replace("package", "", 1).strip().rstrip(";")

    type_nodes = {
        "class_declaration": "class",
        "interface_declaration": "interface",
        "enum_declaration": "enum",
        "record_declaration": "record",
    }

    def visit(node):
        if node.type in type_nodes:
            name_node = node.child_by_field_name("name")
            if name_node is None:
                return
            name = text_of(name_node, source)
            qualified = f"{package}.{name}" if package else name
            kind = type_nodes[node.type]
            item = UMLClass(name=name, qualified_name=qualified, kind=kind)

            modifiers = modifiers_text(node, source)
            body = node.child_by_field_name("body")

            # Extends / implements clauses.
            for child in node.named_children:
                if child.type == "superclass":
                    raw = text_of(child, source)
                    raw = re.sub(r"^\s*extends\s+", "", raw).strip()
                    item.bases.append((clean_type(raw), "inheritance"))
                elif child.type in {"super_interfaces", "extends_interfaces"}:
                    raw = text_of(child, source)
                    raw = re.sub(r"^\s*(implements|extends)\s+", "", raw).strip()
                    for part in raw.split(","):
                        if part.strip():
                            relation = "realization" if kind != "interface" else "inheritance"
                            item.bases.append((clean_type(part), relation))

            if body:
                for member in body.named_children:
                    member_mods = modifiers_text(member, source)
                    prefix = visibility(member_mods)
                    static = " {static}" if "static" in member_mods else " "
                    abstract = " {abstract}" if "abstract" in member_mods else ""

                    if member.type == "field_declaration":
                        type_node = member.child_by_field_name("type")
                        type_name = clean_type(text_of(type_node, source))
                        declarators = [
                            c for c in member.named_children
                            if c.type == "variable_declarator"
                        ]
                        for decl in declarators:
                            n = decl.child_by_field_name("name")
                            field_name = text_of(n, source) if n else "?"
                            item.fields.append(
                                f"{prefix}{field_name}: {type_name}{static.strip()}"
                            )
                        item.field_types.update(simple_type_names(type_name))

                    elif member.type in {"method_declaration", "constructor_declaration"}:
                        name_node = member.child_by_field_name("name")
                        method_name = text_of(name_node, source) if name_node else "?"
                        params_node = member.child_by_field_name("parameters")
                        params = []
                        if params_node:
                            for param in params_node.named_children:
                                if param.type in {"formal_parameter", "spread_parameter",
                                                  "receiver_parameter"}:
                                    p_name = param.child_by_field_name("name")
                                    p_type = param.child_by_field_name("type")
                                    params.append(
                                        f"{text_of(p_name, source) if p_name else 'arg'}: "
                                        f"{clean_type(text_of(p_type, source))}"
                                    )
                        return_node = member.child_by_field_name("type")
                        return_type = clean_type(text_of(return_node, source)) if return_node else ""
                        signature = f"{prefix}{method_name}({', '.join(params)})"
                        if return_type:
                            signature += f": {return_type}"
                        signature += static + abstract
                        item.methods.append(signature)

            result.append(item)
            # Nested classes are included too.
            if body:
                for child in body.named_children:
                    visit(child)
            return

        for child in node.named_children:
            visit(child)

    visit(tree.root_node)
    return result


def discover_sources(project: Path, include_nested: bool = False) -> list[Path]:
    source_root = project / "src" / "main" / "java"
    if not source_root.is_dir():
        raise FileNotFoundError(
            f"Cannot find {source_root}. Expected a Maven project with src/main/java."
        )

    files = []
    for path in source_root.rglob("*.java"):
        relative_parts = {p.lower() for p in path.relative_to(source_root).parts}
        if relative_parts & {"test", "tests", "testing"}:
            continue
        if re.search(r"(test|tests|testing)$", path.stem, re.IGNORECASE):
            continue
        if not include_nested and any(p.startswith(".") for p in path.parts):
            continue
        files.append(path)
    return sorted(files)


def resolve_name(raw: str, known: dict[str, UMLClass]) -> Optional[str]:
    raw = clean_type(raw)
    # Remove generic parameters for direct base-type lookup.
    raw = re.sub(r"<.*>", "", raw)
    raw = raw.replace("[]", "")
    if raw in known:
        return raw
    simple = raw.split(".")[-1]
    matches = [q for q, cls in known.items() if cls.name == simple]
    return matches[0] if len(matches) == 1 else None


def build_relationships(classes: dict[str, UMLClass]) -> list[Relationship]:
    relationships: list[Relationship] = []
    seen = set()

    def add(source, target, kind):
        if source == target:
            return
        key = (source, target, kind)
        if key not in seen:
            seen.add(key)
            relationships.append(Relationship(source, target, kind))

    for qname, cls in classes.items():
        for base, kind in cls.bases:
            target = resolve_name(base, classes)
            if target:
                add(qname, target, kind)

        for type_name in cls.field_types:
            target = resolve_name(type_name, classes)
            if target:
                add(qname, target, "association")

    return relationships


def graphviz_positions(classes: dict[str, UMLClass],
                       relationships: list[Relationship]) -> Optional[dict[str, tuple[float, float]]]:
    dot = shutil.which("dot")
    if not dot:
        return None

    lines = ["digraph UML {", "graph [rankdir=LR, nodesep=0.55, ranksep=1.0];",
             "node [shape=box, fixedsize=false];"]
    ids = {name: f"n{i}" for i, name in enumerate(classes)}
    for name, cls in classes.items():
        label = cls.name.replace('"', "'")
        lines.append(f'{ids[name]} [label="{label}"];')
    for rel in relationships:
        lines.append(f"{ids[rel.source]} -> {ids[rel.target]} [style=dotted];")
    lines.append("}")

    try:
        output = subprocess.run(
            [dot, "-Tplain"], input="\n".join(lines), text=True,
            capture_output=True, check=True
        ).stdout
    except (subprocess.SubprocessError, OSError):
        return None

    raw_positions = {}
    for line in output.splitlines():
        parts = line.split()
        if parts and parts[0] == "node" and len(parts) >= 4:
            node_id, x, y = parts[1], float(parts[2]), float(parts[3])
            name = next(k for k, v in ids.items() if v == node_id)
            raw_positions[name] = (x, y)

    if len(raw_positions) != len(classes):
        return None

    # Graphviz uses a bottom-left origin; draw.io uses a top-left origin.
    min_x = min(x for x, _ in raw_positions.values())
    max_y = max(y for _, y in raw_positions.values())
    return {
        name: ((x - min_x) * 150, (max_y - y) * 150)
        for name, (x, y) in raw_positions.items()
    }


def make_label(cls: UMLClass) -> str:
    def row(content: str, bold: bool = False) -> str:
        tag = "b" if bold else "font"
        if bold:
            return f"<tr><td align='center'><b>{content}</b></td></tr>"
        return f"<tr><td align='left'>{content}</td></tr>"

    header = f"{cls.kind.upper()} {html.escape(cls.name)}"
    fields = [html.escape(x) for x in cls.fields]
    methods = [html.escape(x) for x in cls.methods]
    rows = [row(header, True)]
    rows.append("<tr><td><hr/></td></tr>")
    rows.extend(row(x) for x in fields)
    rows.append("<tr><td><hr/></td></tr>")
    rows.extend(row(x) for x in methods)
    return "<table border='0' cellpadding='4' cellspacing='0'>" + "".join(rows) + "</table>"


def generate_drawio(classes: dict[str, UMLClass],
                    relationships: list[Relationship],
                    output_file: Path):
    positions = graphviz_positions(classes, relationships)
    if positions is None:
        # Predictable fallback layout: four columns.
        positions = {
            name: ((i % 4) * 340, (i // 4) * 260)
            for i, name in enumerate(classes)
        }

    mxfile = ET.Element("mxfile", {
        "host": "app.diagrams.net",
        "modified": "2026-11-20T00:00:00.000Z",
        "agent": "Java UML Generator",
        "version": "24.7.17",
        "type": "device",
    })
    diagram = ET.SubElement(mxfile, "diagram", {"id": "java-uml", "name": "Java UML"})
    model = ET.SubElement(diagram, "mxGraphModel", {
        "dx": "1200", "dy": "800", "grid": "1", "gridSize": "10",
        "guides": "1", "tooltips": "1", "connect": "1", "arrows": "1",
        "fold": "1", "page": "1", "pageScale": "1",
        "pageWidth": "1654", "pageHeight": "1169", "math": "0", "shadow": "0",
    })
    root = ET.SubElement(model, "root")
    ET.SubElement(root, "mxCell", {"id": "0"})
    ET.SubElement(root, "mxCell", {"id": "1", "parent": "0"})

    cell_ids = {}
    for index, (qname, cls) in enumerate(classes.items(), start=2):
        cell_id = str(index)
        cell_ids[qname] = cell_id
        x, y = positions[qname]
        label = make_label(cls)
        style = (
            "rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;"
            "spacing=6;fillColor=#dae8fc;strokeColor=#6c8ebf;"
            "fontSize=12;overflow=hidden;"
        )
        cell = ET.SubElement(root, "mxCell", {
            "id": cell_id, "value": label, "style": style,
            "vertex": "1", "parent": "1",
        })
        geometry = ET.SubElement(cell, "mxGeometry", {
            "x": str(round(x)), "y": str(round(y)),
            "width": "260", "height": str(max(100, 76 + 22 * (len(cls.fields) + len(cls.methods)))),
            "as": "geometry",
        })

    next_id = len(cell_ids) + 2
    for rel in relationships:
        if rel.source not in cell_ids or rel.target not in cell_ids:
            continue
        if rel.kind == "inheritance":
            style = "endArrow=block;endFill=0;html=1;rounded=0;"
        elif rel.kind == "realization":
            style = "endArrow=block;endFill=0;dashed=1;html=1;rounded=0;"
        else:
            style = "endArrow=none;startArrow=none;html=1;rounded=0;"
        edge = ET.SubElement(root, "mxCell", {
            "id": str(next_id), "value": "", "style": style,
            "edge": "1", "parent": "1",
            "source": cell_ids[rel.source], "target": cell_ids[rel.target],
        })
        ET.SubElement(edge, "mxGeometry", {"relative": "1", "as": "geometry"})
        next_id += 1

    ET.indent(mxfile, space="  ")
    output_file.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(mxfile).write(output_file, encoding="utf-8", xml_declaration=True)


def open_diagram(path: Path):
    try:
        if sys.platform.startswith("win"):
            os.startfile(str(path))  # type: ignore[attr-defined]
        elif sys.platform == "darwin":
            subprocess.Popen(["open", str(path)])
        else:
            subprocess.Popen(["xdg-open", str(path)])
    except Exception as exc:
        print(f"Could not automatically open the diagram: {exc}")
        print(f"Open it manually: {path}")


def export_png(drawio_file: Path, png_file: Path):
    candidates = ["drawio", "draw.io"]
    executable = next((shutil.which(x) for x in candidates if shutil.which(x)), None)
    if not executable:
        print("draw.io Desktop CLI not found; export PNG from the editor using File > Export as > PNG.")
        return
    try:
        subprocess.run(
            [executable, "--export", "--format", "png",
             "--output", str(png_file), "--embed-diagram", str(drawio_file)],
            check=True
        )
        print(f"PNG exported: {png_file}")
    except (subprocess.SubprocessError, OSError) as exc:
        print(f"Automatic PNG export failed: {exc}")
        print("Open the .drawio file and use File > Export as > PNG.")


def main():
    parser = argparse.ArgumentParser(description="Generate an editable UML class diagram from a Java Maven project.")
    parser.add_argument("project", type=Path, help="Path to the Maven project root")
    parser.add_argument("--output", type=Path, default=None, help="Output directory (default: project/uml)")
    parser.add_argument("--open", action="store_true", help="Open the generated .drawio file")
    parser.add_argument("--png", action="store_true", help="Also export PNG using draw.io Desktop CLI")
    parser.add_argument("--include-nested", action="store_true", help="Include hidden directories under src/main/java")
    args = parser.parse_args()

    project = args.project.expanduser().resolve()
    if not project.is_dir():
        raise SystemExit(f"Project directory does not exist: {project}")

    output_dir = (args.output or (project / "uml")).expanduser().resolve()
    output_file = output_dir / f"{project.name}_class_diagram.drawio"
    png_file = output_dir / f"{project.name}_class_diagram.png"

    files = discover_sources(project, args.include_nested)
    if not files:
        raise SystemExit("No production Java source files found under src/main/java.")

    java_parser = get_parser("java")
    classes: dict[str, UMLClass] = {}
    for file in files:
        for cls in parse_java_file(file, java_parser):
            if cls.qualified_name in classes:
                print(f"Warning: duplicate class name ignored: {cls.qualified_name}")
            else:
                classes[cls.qualified_name] = cls

    if not classes:
        raise SystemExit("No Java class/interface/enum/record declarations found.")

    relationships = build_relationships(classes)
    generate_drawio(classes, relationships, output_file)

    print(f"Scanned Java files: {len(files)}")
    print(f"UML types: {len(classes)}")
    print(f"Relationships: {len(relationships)}")
    print(f"Editable diagram: {output_file}")

    if args.png:
        export_png(output_file, png_file)
    if args.open:
        open_diagram(output_file)


if __name__ == "__main__":
    main()
