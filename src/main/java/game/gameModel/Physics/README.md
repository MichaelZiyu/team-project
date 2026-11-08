# Physics Engine – Technical Details

## Major Challenges

The physics engine needs to address three major challenges:

- How to make entities move smoothly on the ground.
- How to handle collisions between entities.
- How to handle collisions between entities and blocks.

## How Do Collisions Occur?

If collisions are detected only when two collision boxes physically intersect, entities may partially penetrate the ground before the collision is detected. This can result in unstable movement and may require repeatedly pushing entities away from the ground.

To address this issue, we use a **collision margin**.

## Entity–Block Collision

Collision detection cannot rely solely on exact coordinate matching between collision boxes. In practice, entities may move across a collision boundary between consecutive physics updates.

To handle this, each block body maintains a collision margin around its actual collision boundary.

Whenever an entity's collision box intersects the block's collision margin, we treat it as a potential collision and adjust the entity's position to the precise contact position, as if the collision had occurred at the actual block boundary.

The collision margin must be sufficiently small so that the positional correction is imperceptible to the player.

After positional correction, we apply the corresponding effects to the entity's velocity. These effects are determined by checking actual contact between the entity's collision box and the block's physical boundary, rather than the expanded collision margin.

This separates collision anticipation from actual collision response:

- **Collision margin:** Detects potential collisions early and prevents entities from penetrating blocks.
- **Actual collision boundary:** Determines the physical contact and the corresponding velocity response.

## Entity–Entity Collision

Entity–entity collision is relatively straightforward.

Whenever two entities' collision boxes intersect, we apply a repulsive force between them based on their relative positions.

The direction of the repulsive force is determined by the relative positions of the two entities, pushing them away from each other.