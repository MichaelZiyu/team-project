public class Main {

    public static void main(String[] args){
        FSM controller = new FSM(StartMenuState());
        controller.run();
    }
}
