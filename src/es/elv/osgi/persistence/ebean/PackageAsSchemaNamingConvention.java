package es.elv.osgi.persistence.ebean;

import com.avaje.ebean.config.TableName;
import com.avaje.ebean.config.UnderscoreNamingConvention;

public class PackageAsSchemaNamingConvention extends UnderscoreNamingConvention {

	@Override
	public String getSequenceFormat() {
		return "{table}_id_seq";
	}

	@Override
	public String getSequenceName(String tableName, String pkColumn) {
		return tableName + "_id_seq";
	}

	@Override
	public TableName getTableNameByConvention(Class<?> beanClass) {
		TableName s = super.getTableNameByConvention(beanClass);

		String schema = beanClass.getPackage().getName().replace(".", "_").toLowerCase();

		return new TableName(s.getCatalog(), schema, s.getName());
	}
}
