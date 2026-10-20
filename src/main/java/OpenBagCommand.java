public class OpenBagCommand extends Command {

    @Override
    public void execute(World world) {
        StaticView.inventoryPanel(world.getPlayer().getInventory());
    }
}
