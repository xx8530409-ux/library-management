package com.library.dao;

import com.library.exception.DataAccessException;
import com.library.model.Member;
import com.library.model.MemberType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ★★ 對DB做連接， DAO = Data Access Object，對資料庫裡的資料-MEMBER 做存取的物件
 * {@link Member} 的資料存取物件。CRUD 皆使用 PreparedStatement。
 */
public class MemberDAO {

    /** 新增會員，回填主鍵。 */
    public void insert(Member member) {
        String sql = """
                INSERT INTO members (member_no, name, type, email)
                VALUES (?, ?, ?, ?)""";
        try (Connection conn = DBUtil.getConnection(); ////跟DB拿Connection跟資料庫的連線
             PreparedStatement ps = conn.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getMemberNo());
            ps.setString(2, member.getName());
            ps.setString(3, member.getType().name());
            ps.setString(4, member.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    member.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("新增會員失敗", e);
        }
    }

    /** 依主鍵查詢。 */
    public Optional<Member> findById(long id) {
        return queryOne("SELECT * FROM members WHERE id = ?", id);
    }

    /** 依會員編號查詢（用於重複檢查）。 */
    public Optional<Member> findByMemberNo(String memberNo) {
        String sql = "SELECT * FROM members WHERE member_no = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberNo);

            System.out.println("SQL -> " + ps.toString()); //印出去查詢的SQL語句

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty(); //Optional是JAVA語法，如果要查的東西有就會回傳Optional，沒有就會回傳空 =>MemberMenu
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢會員失敗", e);
        }
    }

    /** 全部會員，依會員編號排序。 */
    public List<Member> findAll() {
        String sql = "SELECT * FROM members ORDER BY member_no";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Member> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("列出會員失敗", e);
        }
    }

    private Optional<Member> queryOne(String sql, long id) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢會員失敗", e);
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(
                rs.getLong("id"),
                rs.getString("member_no"),
                rs.getString("name"),
                MemberType.valueOf(rs.getString("type")),
                rs.getString("email"));
    }
}
