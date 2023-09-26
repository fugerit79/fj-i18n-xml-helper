package test.org.fugerit.java.tool.i18n.xml;

import static org.junit.Assert.fail;

import java.util.Properties;

import org.fugerit.java.core.cfg.ConfigRuntimeException;
import org.fugerit.java.core.util.result.Result;
import org.fugerit.java.tool.i18n.xml.I18NXmlHelper;
import org.junit.Assert;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestI18NXmlHelper {

	@Test
	public void testConvert() {
		Properties params = new Properties();
		params.setProperty( I18NXmlHelper.ARG_CONVERT_CONFIG , "src/test/resources/config/test/convert-config.xml" );
		params.setProperty( I18NXmlHelper.ARG_INPUT_XML , "src/test/resources/config/test/test1.xml" );
		params.setProperty( I18NXmlHelper.ARG_OUTPUT_XML , "target/test1_xml.xml" );
		params.setProperty( I18NXmlHelper.ARG_OUTPUT_PROPERTIES , "target/test1_properties.xml" );
		try {
			int res = I18NXmlHelper.handle( params );
			Assert.assertEquals( Result.RESULT_CODE_OK , res );	
		} catch (ConfigRuntimeException e) {
			log.error( "Error "+e, e );
			fail( "Error : "+e );
		}
	}
	
}
