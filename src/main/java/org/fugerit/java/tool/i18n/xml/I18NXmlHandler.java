package org.fugerit.java.tool.i18n.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;

import org.fugerit.java.core.cfg.ConfigException;
import org.fugerit.java.core.cfg.xml.FactoryCatalog;
import org.fugerit.java.core.cfg.xml.FactoryType;
import org.fugerit.java.core.cfg.xml.GenericListCatalogConfig;
import org.fugerit.java.core.function.SafeFunction;
import org.fugerit.java.core.lang.helpers.ClassHelper;
import org.fugerit.java.core.util.result.Result;
import org.fugerit.java.core.xml.XMLException;
import org.fugerit.java.core.xml.dom.DOMIO;
import org.fugerit.java.tool.i18n.xml.convert.rules.ConvertRule;
import org.fugerit.java.tool.i18n.xml.convert.rules.RuleContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class I18NXmlHandler {

	private void writeCleanXml( PrintWriter writer, String content ) throws IOException {
		try ( BufferedReader cleaner = new BufferedReader( new StringReader( content ) ) ) {
			// maybe not the most elegant way but it does the job.
			cleaner.lines().forEach( l -> {
					// all lines containing only whit spaces or tabs are not written to the output.
					if ( !l.replace( "\t" , "").trim().isEmpty() ) {
						writer.println( l );
					}
				});
		}
	}
	
	private void handleXmlFile( I18NXmlContext context, File inputXml, File outputXml, RuleContext ruleContext, Collection<FactoryType> rules ) throws IOException, XMLException, ClassNotFoundException, NoSuchMethodException, ConfigException {
		log.info( "handle file : {} -> {}", inputXml, outputXml );
		if ( inputXml.isDirectory() ) {
			for ( File currentXml : inputXml.listFiles( context.getFileFilter() ) ) {
				log.debug( "creates output folder : {}", outputXml.mkdir() );
				this.handleXmlFile(context, currentXml, new File( outputXml, currentXml.getName() ) , ruleContext, rules);
			}
		} else {
			try ( FileInputStream fis = new FileInputStream( inputXml);
					StringWriter outXmlBuffer = new StringWriter();
					PrintWriter writer = new PrintWriter( new OutputStreamWriter( new FileOutputStream( outputXml ) ) ) )  {
				Document doc = DOMIO.loadDOMDoc( fis );
				Element root = doc.getDocumentElement();
				ruleContext.setDocument(doc);
				for ( FactoryType ruleConfig : rules ) {
					log.info( "ruleConfig : {} - {}", ruleConfig.getId(), ruleConfig.getType() );
					ConvertRule currentRule = (ConvertRule) ClassHelper.newInstance( ruleConfig.getType() );
					if ( ruleConfig.getElement() != null ) {
						NodeList kids = ruleConfig.getElement().getElementsByTagName( "config" );
						if ( kids.getLength() > 0 ) {
							currentRule.config( (Element) kids.item( 0 ) );	
						}
					}
					currentRule.apply(root, ruleContext);
				}
				// write output xml
				DOMIO.writeDOMIndent( doc , outXmlBuffer );
				this.writeCleanXml(writer, outXmlBuffer.toString());
			}
		}
	}
	
	public int handle( I18NXmlContext context ) {
		return SafeFunction.get( () -> {
			int res = Result.RESULT_CODE_OK;
			FactoryCatalog rulesCatalog = new FactoryCatalog();
			try ( FileInputStream fis = new FileInputStream( new File( context.getConvertConfig() ) ) ) {
				GenericListCatalogConfig.load( fis , rulesCatalog );	
			}
			RuleContext ruleContext = new RuleContext();
			Collection<FactoryType> rules = context.getRuleCatalog( rulesCatalog );
			File inputXml = new File( context.getInputXml() );
			File outputXml = new File( context.getOutputXml() );
			if ( inputXml.isDirectory() ) {
				// check if both input and output parameters are folder.
				if ( outputXml.exists() && !outputXml.isDirectory() ) {
					throw new XMLException( "If input is a directory, output should be a folder too : "+outputXml );
				} else if ( !outputXml.exists() ) {
					// if output folder does not exists create it
					log.info( "creates output directory : {}", outputXml.mkdirs() );
				}
			}
			// recurse xml
			this.handleXmlFile(context, inputXml, outputXml, ruleContext, rules);
			// write output properties
			try ( FileOutputStream fosProps = new FileOutputStream( new File( context.getOutputProperties() ) ) ) {
				ruleContext.saveEntries(fosProps);
			}
			return res;
		} );
	}
	
}
