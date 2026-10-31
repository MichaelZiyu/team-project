package states;

import javax.swing.*;

/**
 * TitleState represents states that are in the title pages
 */
public class PanelState extends State{

    public JPanel panel;

    public PanelState(String name, JPanel panel){
        super(name);
        this.panel = panel;
        PanelStateController.register(name, this);
    }
}
