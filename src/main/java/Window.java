
import javax.swing.*;

public class Window extends JFrame {

    public static final int WINDOW_WIDTH = 700;
    public static final int WINDOW_HEIGHT= 500;

    public Window() {
        this.setLayout(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(WINDOW_WIDTH,WINDOW_HEIGHT);
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        WebScrapping webScrapping = new WebScrapping(0,0,WINDOW_WIDTH,WINDOW_HEIGHT);
        this.add(webScrapping);
        this.setVisible(true);

    }

    public static void main(String[] args) {
        Window main = new Window();
    }
}
