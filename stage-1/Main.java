import java.util.Scanner;

// Entry point of the game: wires the boundary (GameCLI) to the controller and starts it
public class Main{
    public static void main(String[] args){
        GameCLI cli=new GameCLI(new Scanner(System.in));
        GameController controller=new GameController(cli);
        controller.startGame();
    }
}
