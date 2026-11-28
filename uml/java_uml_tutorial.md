# Tutorial: Generate an Editable UML Class Diagram from a Java Maven Project

This guide explains how to scan a Java Maven project, generate an
editable UML class diagram, open it in diagrams.net (draw.io), and
export it as a PNG.

## 1. Download the generator

Download [java_uml.py](sandbox:/mnt/data/java_uml.py) and save it
somewhere convenient. The script can be kept outside the Maven project;
you provide the project folder when running it.

## 2. Install the prerequisites

### Python

Install Python 3.10 or later. On Windows, enable **Add Python to PATH**
if the installer offers that option. Verify the installation:

``` bash
python --version
```

### Tree-sitter Java parser

Install the Python package used to parse Java source code:

``` bash
python -m pip install tree-sitter-language-pack
```

### Graphviz

Graphviz calculates the initial positions of classes in the diagram.

1.  Download it from <https://graphviz.org/download/>.
2.  Install it for your operating system.
3.  Ensure its `bin` directory is on your system `PATH`.
4.  Open a new terminal and verify:

``` bash
dot -V
```

If `dot` is not recognized, fix the `PATH` configuration. The script can
still run without Graphviz, but it will use a basic grid layout.

### diagrams.net Desktop

Install diagrams.net Desktop from <https://www.drawio.com/>. You can
also use the web editor at <https://app.diagrams.net/>.

The desktop version is recommended for opening local `.drawio` files and
using its command-line export feature.

## 3. Understand the expected project structure

The script expects a standard Maven source directory:

``` text
YourProject/
├── pom.xml
└── src/
    ├── main/
    │   └── java/
    │       └── ...
    └── test/
        └── java/
            └── ...
```

It scans `src/main/java`, not `src/test/java`. It also skips directories
named `test`, `tests`, or `testing`, and Java filenames ending in
`Test`, `Tests`, or `Testing` (case-insensitive).

**Limitations:** The current version does not automatically discover
custom source roots declared in `pom.xml`, generated sources, or every
multi-module Maven layout. For a multi-module project, run it separately
for each module containing its own `src/main/java`.

## 4. Generate the diagram

Open PowerShell, Command Prompt, or a terminal.

### Windows

``` powershell
python "C:\path\to\java_uml.py" "C:\Users\YourUser\IdeaProjects\YourProject" --open
```

### macOS / Linux

``` bash
python /path/to/java_uml.py /path/to/YourProject --open
```

Replace the example paths with your actual script and Maven project
paths.

The `--open` option asks your operating system to open the generated
`.drawio` file using the associated application.

### Choose an output directory

By default, the script creates a folder named `uml` inside the project
directory. To choose another location, use `--output`:

``` powershell
python "C:\path\to\java_uml.py" "C:\path\to\YourProject" --output "C:\UML" --open
```

### Generate a PNG as well

``` powershell
python "C:\path\to\java_uml.py" "C:\path\to\YourProject" --open --png
```

The `--png` option looks for a `drawio` or `draw.io` command-line
executable. If it cannot find one, the script still generates the
editable diagram and tells you to export the PNG manually.

The usual output is:

``` text
YourProject/
└── uml/
    ├── YourProject_class_diagram.drawio
    └── YourProject_class_diagram.png
```

If you use `--output`, both files are placed in the specified directory.
