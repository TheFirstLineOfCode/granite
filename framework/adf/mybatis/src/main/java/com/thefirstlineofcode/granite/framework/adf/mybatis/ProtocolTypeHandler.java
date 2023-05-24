package com.thefirstlineofcode.granite.framework.adf.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.thefirstlineofcode.basalt.xmpp.core.Protocol;

@MappedTypes(Protocol.class)
public class ProtocolTypeHandler extends BaseTypeHandler<Protocol> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Protocol parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, parameter.getProtocolString());
	}

	@Override
	public Protocol getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String result = rs.getString(columnName);
		
		return result == null ? null : Protocol.parse(result);
	}

	@Override
	public Protocol getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String result = rs.getString(columnIndex);
		
		return result == null ? null : Protocol.parse(result);
	}

	@Override
	public Protocol getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String result = cs.getString(columnIndex);
		
		return result == null ? null : Protocol.parse(result);
	}

}
