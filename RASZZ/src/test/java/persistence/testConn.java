package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class testConn {
    public static void main(String[] args){
        Connection con = null;
        try{
            Class.forName("org.postgresql.Driver");
            String url =  "jdbc:postgresql://localhost:5432/szz";
            con = DriverManager.getConnection(url, "postgres", "1234");
            String sql = "select * from linkedissuegit";
            Statement stmt = null;
            ResultSet result = null;
            stmt = con.createStatement();
            result = stmt.executeQuery(sql);
            while(result.next()){
                System.out.println(result.getString(1));
            }
        } catch (Exception ex){

        }
    }
}
