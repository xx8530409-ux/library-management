package com.library.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

//Connection 一條與資料庫的連線通道，供給其他獲取資料庫資料使用

public class DBUtil {
    private static final String url;
	private static final String user;
	private static final String password;
	private DBUtil() {
	// 工具類別，不可實例化
	}

	static { 
	    Properties props = new Properties();
	    try (InputStream in = DBUtil.class.getClassLoader() //之後都會是class檔，所以去這個class所在去讀取db.properties(對db存取的參數)
	            .getResourceAsStream("db.properties")) {
	        if (in != null) {
	            props.load(in);
	            }
	    } catch (IOException e) { // 如果有錯誤、讀不到，就改用預設
	        System.err.println("[DBUtil] 讀取 db.properties 失敗，改用預設值：" + e.getMessage());
	    }
            //預設值
	        url = props.getProperty("db.url",
	                "jdbc:mysql://localhost:3306/library_db"
	                + "?useSSL=false&serverTimezone=Asia/Taipei"
	                + "&characterEncoding=utf8&allowPublicKeyRetrieval=true");
	        user = props.getProperty("db.user", "root");
	        password = props.getProperty("db.password", "");
	    }

	    public static Connection getConnection() throws SQLException {
	        return DriverManager.getConnection(url, user, password);  //DriverManager 從 mysql-connector-j.jar 取得 
	}
}
