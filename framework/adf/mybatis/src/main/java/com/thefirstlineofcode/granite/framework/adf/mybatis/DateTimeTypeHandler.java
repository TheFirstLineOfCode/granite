package com.thefirstlineofcode.granite.framework.adf.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.thefirstlineofcode.basalt.xmpp.datetime.DateTime;

@MappedTypes(DateTime.class)
public class DateTimeTypeHandler extends BaseTypeHandler<DateTime> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, DateTime parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, parameter.toString());
	}

	@Override
	public DateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String result = rs.getString(columnName);
		
		try {
			return result == null ? null : DateTime.parse(result);
		} catch (ParseException e) {
			throw new SQLException("Malformated datetime.", e);
		}
	}

	@Override
	public DateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String result = rs.getString(columnIndex);
		
		try {
			return result == null ? null : DateTime.parse(result);
		} catch (ParseException e) {
			throw new SQLException("Malformated datetime.", e);
		}
	}

	@Override
	public DateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String result = cs.getString(columnIndex);
		
		try {
			return result == null ? null : DateTime.parse(result);
		} catch (ParseException e) {
			throw new SQLException("Malformated datetime.", e);
		}
	}

}
