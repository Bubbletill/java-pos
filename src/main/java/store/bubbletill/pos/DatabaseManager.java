package store.bubbletill.pos;

import store.bubbletill.commons.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseManager {

    private final String url = "jdbc:mysql://localhost:3306/bubbletill";
    private final String username;
    private final String password;
    private final LocalData localData;

    public DatabaseManager(String username, String password, LocalData localData) {
        this.username = username;
        this.password = password;
        this.localData = localData;
    }

    public ArrayList<CategoryData> getCategories() {
        ArrayList<CategoryData> toReturn = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM categories");
            while (rs.next()) {
                toReturn.add(new CategoryData(rs.getInt("id"), rs.getString("description")));
            }

            return toReturn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<StockData> getStock() {
        ArrayList<StockData> toReturn = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT category, itemCode, description, price FROM stock");
            while (rs.next()) {
                toReturn.add(new StockData(rs.getInt("category"), rs.getInt("itemCode"), rs.getString("description"), rs.getDouble("price")));
            }

            return toReturn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getTransactionNumber() {
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            PreparedStatement statement = conn.prepareStatement("SELECT trans FROM transactions WHERE `store` = ? AND `register` = ? ORDER BY trans DESC");
            statement.setInt(1, localData.getStore());
            statement.setInt(2, localData.getReg());

            ResultSet rs = statement.executeQuery();
            if (rs.next())
                return rs.getInt("trans");

            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -50;
        }
    }

    public ArrayList<OperatorData> getOperators() {
        ArrayList<OperatorData> toReturn = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM operators");
            while (rs.next()) {
                toReturn.add(new OperatorData(rs.getString("id"), rs.getString("name"), rs.getString("password"), rs.getString("groups")));
            }

            return toReturn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public HashMap<Integer, OperatorGroup> getOperatorGroups() {
        HashMap<Integer, OperatorGroup> toReturn = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM operator_groups");
            while (rs.next()) {
                OperatorGroup og = new OperatorGroup(rs.getInt("id"), rs.getString("name"), rs.getString("allowed_pos_actions"), rs.getString("allowed_bo_actions"));
                toReturn.put(og.getId(), og);
            }

            return toReturn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
