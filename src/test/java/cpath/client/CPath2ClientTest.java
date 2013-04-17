package cpath.client;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.junit.Ignore;
import org.junit.Test;

import cpath.client.util.CPathException;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;

/**
 * INFO: when "cPath2Url" Java property is not set,
 * (e.g., -DcPath2Url="http://localhost:8080/cpath-web-service/" 
 * -DcPath2Url="http://webservice.baderlab.org:48080/", -DcPath2Url="http://www.pathwaycommons.org/pc2/")
 * the default cpath2 endpoint PROVIDER_URL is {@link CPath2Client#DEFAULT_ENDPOINT_URL}
 * (e.g., http://www.pathwaycommons.org/pc2/). So, it is possible that the 
 * default (official) service still provides an older cpath2 API than this PC2 client expects.
 * Take care. 
 */
@Ignore //these tests depend on the data, thus disabled by default (not for daily builds)
public class CPath2ClientTest {
	
	@Test
	public final void testConnectionEtc() {
		final CPath2Client client = CPath2Client.newInstance();
		Collection<String> vals = client.getValidTypes();
		assertFalse(vals.isEmpty());
		assertTrue(vals.contains("BioSource"));
	}
	
	
	@Test
	public final void testGetTopPathways() {
		final CPath2Client client = CPath2Client.newInstance();
		
		SearchResponse result = null;
		try {
			result = client.getTopPathways();
		} catch (Exception e) {
			fail(client.getEndPointURL() + " is not compartible with this test " + e);
		}
		
		assertNotNull(result);
		assertFalse(result.getSearchHit().isEmpty());
	}

	
	@Test
	public final void testTraverse() {
		final CPath2Client cl = CPath2Client.newInstance();
		cl.setPath("Named/name");
		
		// must get a result w/o problems
		ServiceResponse resp = null;
        try {
			resp = cl.traverse(Collections.singleton("http://identifiers.org/taxonomy/9606"));
		} catch (CPathException e) {
			fail(e.toString());
		}
		assertTrue(resp instanceof TraverseResponse);
		assertFalse(((TraverseResponse)resp).getTraverseEntry().isEmpty());
		TraverseEntry entry = ((TraverseResponse)resp).getTraverseEntry().get(0);
		// check value(s)
		assertFalse(entry.getValue().isEmpty());
		assertTrue(entry.getValue().contains("Homo sapiens")); //case matters!
		
		// non-exisitng uri in not error, but the corresp. list of values must be empty
		try {
			resp = cl.traverse(Collections.singleton("bla-bla"));
		} catch (CPathException e1) {
			fail("must not throw a CPathException");
		}
		assertTrue(resp instanceof TraverseResponse); //got response
		assertEquals(1, ((TraverseResponse)resp).getTraverseEntry().size()); // got entry for "bla-bla" uri
		entry = ((TraverseResponse)resp).getTraverseEntry().get(0);
		// however, - no values there!
		assertTrue(entry.getValue().isEmpty());

        
		//intentionally wrong path -> failure (error)
		cl.setPath("BioSource/participant"); 
        try {
			resp = cl.traverse(Collections.singleton("http://identifiers.org/taxonomy/9606"));
			fail("must throw CPathException and not something else");
		} catch (CPathException e) {
			//ok
		}
	}
	
	
	@Test
	public final void testSearch() {
		final CPath2Client cl = CPath2Client.newInstance();
        cl.setType("pathway");
		
		ServiceResponse resp = null;
        try {
			resp = cl.search("name:\"bmp signaling pathway\"");
		} catch (CPathException e) {
			fail(e.toString());
		}
		
		assertTrue(resp instanceof SearchResponse);
		assertFalse(((SearchResponse)resp).getSearchHit().isEmpty());
	}
	
	
	@Test //this test is even more dependent on the data there
	public final void testGetByUri() {
		final CPath2Client cl = CPath2Client.newInstance();
		String id = "BRCA2"; 
		
		Model m = cl.get(Collections.singleton(id));
		assertFalse(m == null);
		assertFalse(m.getObjects().isEmpty());
		assertEquals(1, m.getObjects().size()); //xref
		assertTrue(m.getObjects().iterator().next() instanceof UnificationXref);
			
		String q = cl.queryGet(Collections.singleton(id));
		try {
			String res = cl.executeQuery(q, null);
		} catch (CPathException e) {
			fail();
		}
		
	}

	@Test
	//@Ignore
	public final void testPathsBetweenQuery()
	{
		final CPath2Client cl = CPath2Client.newInstance();
		cl.setGraphQueryLimit(1);

		Set<String> source1 = new LinkedHashSet<String>(Arrays.asList(
			"JUN", "PTEN"
		));

		Model model = cl.getPathsBetween(source1);
		System.out.println("model.getObjects(.size()) = " + model.getObjects().size());

		SimpleIOHandler h = new SimpleIOHandler();

		try{h.convertToOWL(model, new FileOutputStream("target/testPathsBetweenQuery.out.owl"));
		} catch (FileNotFoundException e){e.printStackTrace();}
	}
}
