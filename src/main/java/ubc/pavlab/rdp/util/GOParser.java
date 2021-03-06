package ubc.pavlab.rdp.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Relationship;
import ubc.pavlab.rdp.model.enums.Aspect;
import ubc.pavlab.rdp.model.enums.RelationshipType;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Read in the GO OBO file provided by the Gene Ontology Consortium.
 * <p>
 * Created by mjacobson on 17/01/18.
 */

public class GOParser {

    private static Log log = LogFactory.getLog( GOParser.class );

    public static Map<String, GeneOntologyTerm> parse( URL url ) throws ParseException {
        try {
            return parse( url.openStream() );
        } catch (IOException e) {
            throw new ParseException( e.getMessage(), 0 );
        }
    }

    public static Map<String, GeneOntologyTerm> parse( File file ) throws ParseException {
        try {
            return parse( new FileInputStream( file ) );
        } catch (FileNotFoundException e) {
            throw new ParseException( e.getMessage(), 0 );
        }
    }

    private static Map<String, GeneOntologyTerm> parse( InputStream input ) throws ParseException {
        Map<String, GeneOntologyTerm> termMap = new HashMap<>();

        try {
            if ( input.available() == 0 ) {
                throw new ParseException( "Stream contains no data.", 0 );
            }

            BufferedReader br = new BufferedReader( new InputStreamReader( input ) );
            GeneOntologyTerm currentNode = null;
            GeneOntologyTerm parentNode;
            // while ( br.ready() ) {
            String line;
            while ((line = br.readLine()) != null) {
                // String line = br.readLine();

                if ( line.equals( "" ) ) {
                    // Finished/Between Stanza
                    if ( currentNode != null ) {
                        // Just finished Term Stanza
                        // Create Node
                        termMap.put( currentNode.getGoId(), currentNode );
                        currentNode = null;
                    } // Else Finished unimportant Stanza

                } else if ( line.equals( "[Term]" ) ) {
                    // Starting a Term Stanza
                    currentNode = new GeneOntologyTerm();
                } else if ( currentNode != null ) {
                    // Within a Term Stanza
                    String[] tagValuePair = line.split( ": ", 2 );
                    String[] values;
                    switch (tagValuePair[0]) {
                        case "id":
                            if ( !termMap.containsKey( tagValuePair[1] ) ) {
                                currentNode.setGoId( tagValuePair[1] );
                            } else {
                                currentNode = termMap.get( tagValuePair[1] );
                            }

                            break;
                        case "name":
                            currentNode.setName( tagValuePair[1] );
                            break;
                        case "namespace":
                            currentNode.setAspect( Aspect.valueOf( tagValuePair[1] ) );
                            break;
                        case "alt_id":
                            termMap.put( tagValuePair[1], currentNode );
                            break;
                        case "def":
                            currentNode.setDefinition( tagValuePair[1].split( "\"" )[1] );
                            break;
                        case "is_a":
                            values = tagValuePair[1].split( " " );
                            if ( !termMap.containsKey( values[0] ) ) {
                                // parent exists in map
                                parentNode = new GeneOntologyTerm();
                                parentNode.setGoId( values[0] );
                                termMap.put( values[0], parentNode );
                            } else {
                                parentNode = termMap.get( values[0] );
                            }
                            currentNode.addParent( new Relationship( parentNode, RelationshipType.IS_A ) );
                            parentNode.addChild( new Relationship( currentNode, RelationshipType.IS_A ) );
                            break;
                        case "relationship":
                            values = tagValuePair[1].split( " " );
                            if ( values[0].equals( "part_of" ) ) {

                                if ( !termMap.containsKey( values[1] ) ) {
                                    // parent exists in map
                                    parentNode = new GeneOntologyTerm();
                                    parentNode.setGoId( values[1] );
                                    termMap.put( values[1], parentNode );
                                } else {
                                    parentNode = termMap.get( values[1] );
                                }
                                currentNode.addParent( new Relationship( parentNode, RelationshipType.PART_OF ) );
                                parentNode.addChild( new Relationship( currentNode, RelationshipType.PART_OF ) );
                            }
                            break;
                        case "is_obsolete":
                            currentNode.setObsolete( tagValuePair[1].equals( "true" ) );
                            break;
                        default:
                            break;
                    }

                }

            }
        } catch (IOException e) {
            throw new ParseException( e.getMessage(), 0 );
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
                log.error( ex.getMessage() );
            }
        }

        return termMap;

    }

}
