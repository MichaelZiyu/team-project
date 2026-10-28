public class Main {

    public static void main(String[] args){
        FSM controller = new FSM(TitleState());
        // all panels are stored like this:
        // Panels:
        // - StartMenuPanel
        // - SettingMenuPanel
        // States:
        // - StartMenuState(will have information about StartMenuPanel, and transition map)
        // - SettingMenuState(panel and transition map)
        controller.run();
    }
}
