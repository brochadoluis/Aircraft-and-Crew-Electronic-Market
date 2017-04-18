package db_connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * Created by Luis on 18/04/2017.
 */
public class Loader {
    private Connection conn = null;
    private Statement stmt = null;
    private ResultSet rs = null;

    public Loader() {
    }

    /**
     * TODO: add argument (table(s) to consult
     */
    public void establishConnection(String role){
        try {
            // The newInstance() call is a work around for some
            // broken Java implementations
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            // handle the error
        }
        try {
            //user and password should be arguments, but to test they're hardcoded
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/thesis?useSSL=false","root","luisreis_1992");
            fetchDataBase(role);
            // Do something with the Connection
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void endConnection(){
        if(rs != null){
            try {
                rs.close();
            } catch (SQLException sqlEx) { } // ignore
            rs = null;
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException sqlEx) { } // ignore
            stmt = null;
        }

        if(conn != null){
            try {
                conn.close();
            } catch (SQLException sqlEx) { } // ignore
            conn = null;
        }
    }

    public void fetchDataBase(String role){
        try {
            stmt = conn.createStatement();
            if("buyer".equals(role)){
//                rs = stmt.executeQuery("select * from buyer where origin = 'ORY' ");
            }
            if("seller".equals(role)){
//                rs = stmt.executeQuery("select rank,salary from crew_member where `\uFEFFcrew_member_number`= '16358.4' ");
                rs = stmt.executeQuery("select `\uFEFFtail_number` from aircraft where aircraft_model1 = '319' ");
            }
            // Now do something with the ResultSet ....
            handleDataBaseFetchResults();
            endConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleDataBaseFetchResults() {
        if (rs != null) {
            ResultSetMetaData rsmd;
            try {
                rsmd = rs.getMetaData();
                int columnsNumber = rsmd.getColumnCount();
                while (rs.next()) {/*
                    String rank = rs.getNString("rank");
                    int salary = rs.getInt("salary");
                    String status = rs.getNString("status");

                    System.out.println("RANK: " + rank);
                    System.out.println("SALARY: " + salary);
                    System.out.println("STATUS: " + status);*/
                    for (int i = 1; i <= columnsNumber; i++) {
                        String columnValue = rs.getString(i);
                        System.out.println(rsmd.getColumnLabel(i) + " : " + rs.getString(i));
                    }
                    System.out.println("");
                }
            } catch (SQLException e) {
                // handle any errors
                System.out.println("SQLException: " + e.getMessage());
                System.out.println("SQLState: " + e.getSQLState());
                System.out.println("VendorError: " + e.getErrorCode());
            }
        }
    }
}
