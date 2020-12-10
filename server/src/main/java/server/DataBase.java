package server;

import java.sql.*;

public class DataBase {
    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement getName;
    private static PreparedStatement upDateName;
    private static PreparedStatement setStatus;
    private static PreparedStatement getStatus;
    private static PreparedStatement getIdUser;

    public static boolean connect() {
        try{
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chat.db");
            stmt = connection.createStatement();
            allStetements();
            return true;
        } catch (ClassNotFoundException | SQLException e){
            e.printStackTrace();
            return false;
        }

    }


    private static void disconnect() {
        try {
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }



    private static void allStetements() throws SQLException {
        getName = connection.prepareStatement("SELECT name FROM users WHERE login = ? AND password = ?;");
        getIdUser = connection.prepareStatement("SELECT id FROM users WHERE login = ? AND password = ?;");
        upDateName = connection.prepareStatement("UPDATE users SET name=? WHERE id = ?");
        setStatus = connection.prepareStatement("UPDATE users SET status = ? WHERE id = ?;");
        getStatus = connection.prepareStatement("SELECT status FROM users WHERE id = ?;");
    }

    public static String getNameAuthUser(String login,String pass){
        String name = null;
        Integer id = 0;
        try {

        getName.setString(1,login);
        getName.setString(2,pass);
        ResultSet result = getName.executeQuery();
        if (result.next()) {
            name = result.getString("name");

        }
        result.close();
    } catch (SQLException e) {
        e.printStackTrace();
    }
        return name;
    }

    public static Integer getIdUserStetement(String login,String pass){
        Integer id = 0;
        try {

            getIdUser.setString(1,login);
            getIdUser.setString(2,pass);
            ResultSet result = getIdUser.executeQuery();
            if (result.next()) {

                id = result.getInt("id");
            }
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }
    public static boolean updateUserName(Integer id,String newName){
    try{
        upDateName.setString(1,newName);
        upDateName.setInt(2,id);
        upDateName.executeUpdate();
        return true;
    }catch (SQLException e){
        e.printStackTrace();
        return false;
    }
    }
    public static boolean getStatusUser(Integer id){
        boolean status = false;
       try  {
           getStatus.setInt(1,id);
           ResultSet result = getIdUser.executeQuery();
           if (result.next()) {

               status = result.getBoolean("status");
           }
           result.close();

       }catch (SQLException e){
           e.printStackTrace();

       }
            return status;
    }
    public static boolean upDateStatusUser(Integer id) throws SQLException {
        if(!getStatusUser(id)){
            setStatus.setBoolean(1,true);
            upDateName.setInt(2,id);
            upDateName.executeUpdate();
            return true;
        }else {
            setStatus.setBoolean(1,false);
            upDateName.setInt(2,id);
            upDateName.executeUpdate();
            return false;
        }
    }

}
