# Chess

A simple console-based Java chess game.

## Build and run

Requires Java 17 or newer.

```bash
mkdir -p out
javac -d out src/main/java/com/kuurrooko/chess/ChessGame.java
java -cp out com.kuurrooko.chess.ChessGame
```

Enter moves as two squares, such as `e2 e4`. Type `quit` to exit.

## How to Play

1. Build and run the Java application.
2. Start a new game from the main menu.
3. Players take turns moving White and Black pieces.
4. The game ends when one king is checkmated or when players agree to a draw.

## How to Move Pieces

Use standard chess rules for movement:

- **Pawn**: moves forward 1 square (or 2 from the starting position), captures diagonally.
- **Rook**: moves any number of squares horizontally or vertically.
- **Knight**: moves in an L-shape (2 squares in one direction, then 1 perpendicular).
- **Bishop**: moves any number of squares diagonally.
- **Queen**: moves any number of squares horizontally, vertically, or diagonally.
- **King**: moves 1 square in any direction.

Always make legal moves and protect your king from check.
