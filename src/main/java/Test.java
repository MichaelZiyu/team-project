public class Test {

    public static void sleep(int second) { try { Thread.sleep(second * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    public static void print(Object content) {
        System.out.println(content+"");
    }

    public static void print() {
        System.out.println("");
    }
    public static String input() { return new java.util.Scanner(System.in).nextLine(); }

}
