package com.kuurrooko.chess;

import java.util.Scanner;

public final class ChessGame {
    private final Board board = new Board();
    private Color turn = Color.WHITE;

    public static void main(String[] args) {
        new ChessGame().play();
    }

    private void play() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Chess - enter moves such as e2 e4, or 'quit'.");
            while (true) {
                System.out.println();
                System.out.println(board);

                if (board.isCheckmate(turn)) {
                    System.out.println("Checkmate. " + turn.opposite() + " wins.");
                    return;
                }
                if (board.isStalemate(turn)) {
                    System.out.println("Stalemate. Draw.");
                    return;
                }
                if (board.isInCheck(turn)) {
                    System.out.println(turn + " is in check.");
                }

                System.out.print(turn + " to move: ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    return;
                }

                String[] squares = input.split("\\s+");
                if (squares.length != 2) {
                    System.out.println("Enter a source and destination, for example: e2 e4");
                    continue;
                }
                try {
                    if (board.move(Square.parse(squares[0]), Square.parse(squares[1]), turn)) {
                        turn = turn.opposite();
                    } else {
                        System.out.println("Illegal move.");
                    }
                } catch (IllegalArgumentException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    private enum Color {
        WHITE, BLACK;

        Color opposite() {
            return this == WHITE ? BLACK : WHITE;
        }
    }

    private enum Kind {
        KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
    }

    private record Piece(Color color, Kind kind) {
        @Override
        public String toString() {
            char symbol = switch (kind) {
                case KING -> 'K';
                case QUEEN -> 'Q';
                case ROOK -> 'R';
                case BISHOP -> 'B';
                case KNIGHT -> 'N';
                case PAWN -> 'P';
            };
            return String.valueOf(color == Color.WHITE ? symbol : Character.toLowerCase(symbol));
        }
    }

    private record Square(int row, int column) {
        static Square parse(String notation) {
            if (!notation.matches("[a-hA-H][1-8]")) {
                throw new IllegalArgumentException("Invalid square: " + notation);
            }
            return new Square(notation.charAt(1) - '1', Character.toLowerCase(notation.charAt(0)) - 'a');
        }
    }

    private static final class Board {
        private final Piece[][] squares = new Piece[8][8];
        private boolean whiteKingMoved;
        private boolean blackKingMoved;
        private boolean whiteLeftRookMoved;
        private boolean whiteRightRookMoved;
        private boolean blackLeftRookMoved;
        private boolean blackRightRookMoved;

        Board() {
            setup();
        }

        private void setup() {
            Kind[] backRank = {Kind.ROOK, Kind.KNIGHT, Kind.BISHOP, Kind.QUEEN,
                    Kind.KING, Kind.BISHOP, Kind.KNIGHT, Kind.ROOK};
            for (int column = 0; column < 8; column++) {
                squares[0][column] = new Piece(Color.WHITE, backRank[column]);
                squares[1][column] = new Piece(Color.WHITE, Kind.PAWN);
                squares[6][column] = new Piece(Color.BLACK, Kind.PAWN);
                squares[7][column] = new Piece(Color.BLACK, backRank[column]);
            }
        }

        boolean move(Square from, Square to, Color color) {
            Piece piece = at(from);
            if (piece == null || piece.color() != color || !isLegalMove(from, to, color)) {
                return false;
            }
            Board copy = copy();
            copy.apply(from, to);
            if (copy.isInCheck(color)) {
                return false;
            }
            apply(from, to);
            return true;
        }

        private boolean isLegalMove(Square from, Square to, Color color) {
            if (!inside(to) || from.equals(to)) {
                return false;
            }
            Piece piece = at(from);
            Piece destination = at(to);
            if (piece == null || piece.color() != color || (destination != null && destination.color() == color)) {
                return false;
            }
            int rowDelta = to.row() - from.row();
            int columnDelta = to.column() - from.column();
            int rowDistance = Math.abs(rowDelta);
            int columnDistance = Math.abs(columnDelta);

            return switch (piece.kind()) {
                case PAWN -> legalPawnMove(from, to, piece.color(), rowDelta, columnDelta);
                case KNIGHT -> rowDistance * columnDistance == 2;
                case BISHOP -> rowDistance == columnDistance && clearPath(from, to);
                case ROOK -> (rowDelta == 0 || columnDelta == 0) && clearPath(from, to);
                case QUEEN -> (rowDelta == 0 || columnDelta == 0 || rowDistance == columnDistance)
                        && clearPath(from, to);
                case KING -> (rowDistance <= 1 && columnDistance <= 1)
                        || legalCastle(from, to, piece.color());
            };
        }

        private boolean legalPawnMove(Square from, Square to, Color color, int rowDelta, int columnDelta) {
            int direction = color == Color.WHITE ? 1 : -1;
            int startRow = color == Color.WHITE ? 1 : 6;
            boolean capture = at(to) != null;
            if (columnDelta == 0 && !capture && rowDelta == direction) {
                return true;
            }
            if (columnDelta == 0 && !capture && from.row() == startRow && rowDelta == 2 * direction) {
                return at(new Square(from.row() + direction, from.column())) == null;
            }
            return Math.abs(columnDelta) == 1 && rowDelta == direction && capture;
        }

        private boolean legalCastle(Square from, Square to, Color color) {
            int homeRow = color == Color.WHITE ? 0 : 7;
            if (from.row() != homeRow || from.column() != 4 || to.row() != homeRow
                    || Math.abs(to.column() - from.column()) != 2 || isInCheck(color)) {
                return false;
            }
            if (color == Color.WHITE ? whiteKingMoved : blackKingMoved) {
                return false;
            }
            boolean kingSide = to.column() > from.column();
            int rookColumn = kingSide ? 7 : 0;
            Piece rook = at(new Square(homeRow, rookColumn));
            boolean rookMoved = color == Color.WHITE
                    ? (kingSide ? whiteRightRookMoved : whiteLeftRookMoved)
                    : (kingSide ? blackRightRookMoved : blackLeftRookMoved);
            if (rook == null || rook.kind() != Kind.ROOK || rook.color() != color || rookMoved) {
                return false;
            }
            int step = kingSide ? 1 : -1;
            for (int column = from.column() + step; column != rookColumn; column += step) {
                if (at(new Square(homeRow, column)) != null) {
                    return false;
                }
            }
            Board copy = copy();
            copy.apply(from, new Square(homeRow, from.column() + step));
            return !copy.isInCheck(color);
        }

        private void apply(Square from, Square to) {
            Piece piece = at(from);
            boolean castle = piece.kind() == Kind.KING && Math.abs(to.column() - from.column()) == 2;
            set(to, piece);
            set(from, null);
            updateMoved(piece, from);
            if (castle) {
                int rookFromColumn = to.column() > from.column() ? 7 : 0;
                int rookToColumn = to.column() > from.column() ? 5 : 3;
                set(new Square(from.row(), rookToColumn), at(new Square(from.row(), rookFromColumn)));
                set(new Square(from.row(), rookFromColumn), null);
            }
            if (piece.kind() == Kind.PAWN && (to.row() == 0 || to.row() == 7)) {
                set(to, new Piece(piece.color(), Kind.QUEEN));
            }
        }

        private void updateMoved(Piece piece, Square from) {
            if (piece.kind() == Kind.KING) {
                if (piece.color() == Color.WHITE) whiteKingMoved = true;
                else blackKingMoved = true;
            } else if (piece.kind() == Kind.ROOK) {
                if (from.row() == 0 && from.column() == 0) whiteLeftRookMoved = true;
                if (from.row() == 0 && from.column() == 7) whiteRightRookMoved = true;
                if (from.row() == 7 && from.column() == 0) blackLeftRookMoved = true;
                if (from.row() == 7 && from.column() == 7) blackRightRookMoved = true;
            }
        }

        boolean isCheckmate(Color color) {
            return isInCheck(color) && !hasLegalMove(color);
        }

        boolean isStalemate(Color color) {
            return !isInCheck(color) && !hasLegalMove(color);
        }

        boolean isInCheck(Color color) {
            Square king = findKing(color);
            if (king == null) return true;
            Color opponent = color.opposite();
            for (int row = 0; row < 8; row++) {
                for (int column = 0; column < 8; column++) {
                    Square from = new Square(row, column);
                    Piece piece = at(from);
                    if (piece != null && piece.color() == opponent && attacks(from, king)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean attacks(Square from, Square to) {
            Piece piece = at(from);
            int rowDelta = to.row() - from.row();
            int columnDelta = to.column() - from.column();
            int rowDistance = Math.abs(rowDelta);
            int columnDistance = Math.abs(columnDelta);
            return switch (piece.kind()) {
                case PAWN -> rowDelta == (piece.color() == Color.WHITE ? 1 : -1) && columnDistance == 1;
                case KNIGHT -> rowDistance * columnDistance == 2;
                case KING -> rowDistance <= 1 && columnDistance <= 1;
                case BISHOP -> rowDistance == columnDistance && clearPath(from, to);
                case ROOK -> (rowDelta == 0 || columnDelta == 0) && clearPath(from, to);
                case QUEEN -> (rowDelta == 0 || columnDelta == 0 || rowDistance == columnDistance)
                        && clearPath(from, to);
            };
        }

        private boolean hasLegalMove(Color color) {
            for (int fromRow = 0; fromRow < 8; fromRow++) {
                for (int fromColumn = 0; fromColumn < 8; fromColumn++) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toColumn = 0; toColumn < 8; toColumn++) {
                            Square from = new Square(fromRow, fromColumn);
                            Square to = new Square(toRow, toColumn);
                            if (isLegalMove(from, to, color)) {
                                Board copy = copy();
                                copy.apply(from, to);
                                if (!copy.isInCheck(color)) return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        private Square findKing(Color color) {
            for (int row = 0; row < 8; row++) {
                for (int column = 0; column < 8; column++) {
                    if (new Piece(color, Kind.KING).equals(at(new Square(row, column)))) {
                        return new Square(row, column);
                    }
                }
            }
            return null;
        }

        private boolean clearPath(Square from, Square to) {
            int rowStep = Integer.compare(to.row(), from.row());
            int columnStep = Integer.compare(to.column(), from.column());
            int row = from.row() + rowStep;
            int column = from.column() + columnStep;
            while (row != to.row() || column != to.column()) {
                if (at(new Square(row, column)) != null) return false;
                row += rowStep;
                column += columnStep;
            }
            return true;
        }

        private Piece at(Square square) {
            return inside(square) ? squares[square.row()][square.column()] : null;
        }

        private void set(Square square, Piece piece) {
            squares[square.row()][square.column()] = piece;
        }

        private static boolean inside(Square square) {
            return square.row() >= 0 && square.row() < 8 && square.column() >= 0 && square.column() < 8;
        }

        private Board copy() {
            Board copy = new Board();
            for (int row = 0; row < 8; row++) {
                System.arraycopy(squares[row], 0, copy.squares[row], 0, 8);
            }
            copy.whiteKingMoved = whiteKingMoved;
            copy.blackKingMoved = blackKingMoved;
            copy.whiteLeftRookMoved = whiteLeftRookMoved;
            copy.whiteRightRookMoved = whiteRightRookMoved;
            copy.blackLeftRookMoved = blackLeftRookMoved;
            copy.blackRightRookMoved = blackRightRookMoved;
            return copy;
        }

        @Override
        public String toString() {
            StringBuilder output = new StringBuilder("  a b c d e f g h\n");
            for (int row = 7; row >= 0; row--) {
                output.append(row + 1).append(' ');
                for (int column = 0; column < 8; column++) {
                    output.append(at(new Square(row, column)) == null ? "." : at(new Square(row, column)));
                    output.append(' ');
                }
                output.append(row + 1).append('\n');
            }
            return output.append("  a b c d e f g h").toString();
        }
    }
}
