package testpackage;

import system.InputBuffer;

import javax.swing.*;

public class Test {

    public static void sleep(int second) { try { Thread.sleep(second * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    public static void print(Object content) {
        System.out.println(content+"");
    }

    public static void print() {
        System.out.println("");
    }
    public static String input() { return new java.util.Scanner(System.in).nextLine(); }
    public static String input(String words) {
        System.out.print(words);
        return input();
    }

    public static void main(String[] args) {
        InputBuffer input = new InputBuffer();
        JFrame frame = new JFrame("frame");
        frame.setSize(800,600);
        JPanel panel  = new JPanel();
        JTextField text = new JTextField("input");
        panel.add(text);
        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
        frame.setVisible(true);

        while (true) {
            sleep(2);
            System.out.println(input.getInput());
        }
    }

}
