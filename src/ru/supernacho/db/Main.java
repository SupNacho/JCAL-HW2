package ru.supernacho.db;

import java.sql.*;
import java.util.Scanner;

public class Main {
    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement pst;

    private static final  String COST_CHANGE = "/сменитьцену";
    private static final  String BY_COST_RANGE = "/товарыпоцене";
    private static final  String BY_TITLE = "/цена";
    private static final  String VIEW_ALL = "/все";
    private static final  String EXIT = "/выход";
    private static final  String NO_ARGS = "Нехватает аргументов! пример: ";
    private static final  String NO_GOODS = "Такого товара не найдено.";
    private static ResultSet resultSet;

    public static void main(String[] args) {

        try {
            startBase();
            usageInfo();
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String[] input = scanner.nextLine().split(" ");
                String cmd = input[0];
                switch (cmd) {
                    case BY_TITLE:
                        if (input.length < 2) {
                            System.out.println(NO_ARGS + BY_TITLE + " <товар1>");
                            break;
                        }
                        String goods = input[1];
                        viewByTitle(goods);
                        break;

                    case COST_CHANGE:
                        if (input.length < 3) {
                            System.out.println(NO_ARGS + COST_CHANGE + " <товар1> <500>");
                            break;
                        }
                        goods = input[1];
                        int costOne = Integer.parseInt(input[2]);
                        changeCost(goods, costOne);
                        break;

                    case BY_COST_RANGE:
                        if (input.length < 3) {
                            System.out.println(NO_ARGS + BY_COST_RANGE + " <10> <100>");
                            break;
                        }
                        costOne = Integer.parseInt(input[1]);
                        int costTwo = Integer.parseInt(input[2]);
                        goodsInRange(costOne, costTwo);
                        break;

                    case VIEW_ALL:
                        connect();
                        resultSet = stmt.executeQuery("SELECT * FROM goods");
                        while (resultSet.next()) {
                            System.out.println(
                                    "[ " +
                                            resultSet.getString("prodID") + " | " +
                                            resultSet.getString("title") + " | " +
                                            resultSet.getInt("cost") + " ]"
                            );
                        }
                        disconnect();
                        break;

                    case EXIT:
                        System.out.println("Завершение работы.");
                        return;

                    default:
                        usageInfo();
                        break;
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private static void startBase() throws SQLException {
        connect();
        stmt.execute("CREATE TABLE IF NOT EXISTS goods (\n" +
                "    ID     INTEGER PRIMARY KEY AUTOINCREMENT\n" +
                "                   NOT NULL\n" +
                "                   UNIQUE,\n" +
                "    prodID STRING  UNIQUE,\n" +
                "    title  STRING,\n" +
                "    cost   INTEGER\n" +
                ");");
        stmt.executeUpdate("DELETE FROM goods");
        stmt.execute("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='Students';");
        pst = connection.prepareStatement("INSERT INTO goods (prodId, title, cost) VALUES (?, ?, ?)");
        connection.setAutoCommit(false);
        for (int i = 1; i <= 10000 ; i++) {
            pst.setString(1,"id товара " + i);
            pst.setString(2,"товар" + i);
            pst.setInt(3,i*10);
            pst.addBatch();
        }
        pst.executeBatch();
        connection.commit();
        disconnect();
    }

    private static void goodsInRange(int costOne, int costTwo) throws SQLException {
        connect();
        pst = connection.prepareStatement("SELECT title, cost FROM goods WHERE cost >= ? AND cost <= ?;");
        pst.setInt(1,costOne);
        pst.setInt(2,costTwo);
        resultSet = pst.executeQuery();
        if (resultSet.isClosed()) {
            System.out.println(NO_GOODS);
            return;
        }

        System.out.println("Товары в ценовом диапарозне от: " + costOne + " до: " + costTwo);
        while (resultSet.next()) {
            System.out.println(
                    "[ " +  resultSet.getString("title") + " | " +
                            resultSet.getInt("cost") + " ]"
            );
        }
        disconnect();
    }

    private static void changeCost(String goods, int costOne) throws SQLException {
        connect();
        pst = connection.prepareStatement("UPDATE goods SET cost = ? WHERE title = ?;");
        pst.setInt(1,costOne);
        pst.setString(2,goods);
        int chngCnt = pst.executeUpdate();
        pst.close();

        pst = connection.prepareStatement("SELECT title, cost FROM goods WHERE title = ?;");
        pst.setString(1,goods);
        resultSet = pst.executeQuery();
        if (resultSet.isClosed()) {
            System.out.println(NO_GOODS);
            return;
        }

        if (chngCnt > 0) {
            System.out.println(
                    "Изменена цена товара: " +
                            resultSet.getString("title") +
                            " Цена: " +
                            resultSet.getInt("cost")
            );
        } else {
            System.out.println("Товар не найден.");
        }
        disconnect();
    }

    private static void viewByTitle(String goods) throws SQLException {
        connect();
        pst = connection.prepareStatement("SELECT title, cost FROM goods WHERE title = ?;");
        pst.setString(1,goods);

        resultSet = pst.executeQuery();
        if (resultSet.isClosed()) {
            System.out.println(NO_GOODS);
            return;
        }

        System.out.println(
                "Наименование товара: " +
                        resultSet.getString("title") +
                        " Цена: " +
                        resultSet.getInt("cost")
        );
        disconnect();
    }

    private static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:GoodsDB.db");
            stmt = connection.createStatement();
//            System.out.println("Подключение к БД.");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void usageInfo(){
        System.out.println("Для работы используйте следующие комманды:\n" +
                BY_TITLE + " <наименование товара> - для вывода конкретного товара из БД;\n" +
                BY_COST_RANGE + " <int> <int> - для вывода товаров в заданном диапазоне;\n" +
                COST_CHANGE + " <наименование товара> <int - новая цена> - для изменения цены товара;\n" +
                VIEW_ALL + " - вывести список всех товаров в БД;\n" +
                EXIT + " - для выхода из программы.\n");
    }
}