package com.mozilla.grouperfish.model;

import org.testng.annotations.Test;

import com.mozilla.grouperfish.model.Access.Type;
import com.mozilla.grouperfish.service.ConfigurationsResource;
import com.mozilla.grouperfish.service.DocumentsResource;
import com.mozilla.grouperfish.service.QueriesResource;
import com.mozilla.grouperfish.service.ResultsResource;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;


@Test(groups="unit")
public class NamespaceTest {

    private final String NS = "unit-test";
    private final Access DUMMY_ACCESS = new DummyAccess(Type.CREATE, "dummy.example.com");

    public void testCreateNamespace() {
        assertNotNull(Namespace.get("Test"));
    }

    public void testAllows() {
        assertTrue(Namespace.get(NS).allows(DocumentsResource.class, DUMMY_ACCESS));
    }

    public void testExistingConfigurations() {
        for (final String configType : Namespace.CONFIG_TYPES) {
            assertNotNull(Namespace.get(NS).configurations(configType));
        }
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void testInvalidConfigurations() {
        Namespace.get(NS).configurations("other");
    }

    public void testDocuments() {
        assertNotNull(Namespace.get(NS).documents());
    }

    public void testMaxLength() {
        Access access = new DummyAccess(Type.CREATE, "dummy.example.com");
        assertTrue(0 < Namespace.get(NS).maxLength(DocumentsResource.class, access));
    }

    public void testQueries() {
        assertNotNull(Namespace.get(NS).queries());
    }

    public void testResourceMap() {
        Namespace ns = Namespace.get(NS);
        assertEquals(
                ns.documents(), ns.resourceMap(DocumentsResource.class));
        assertEquals(
                ns.queries(), ns.resourceMap(QueriesResource.class));
        assertEquals(
                ns.results(), ns.resourceMap(ResultsResource.class));
        assertEquals(
                ns.configurations("filters"),
                ns.resourceMap(ConfigurationsResource.FilterConfigsResource.class));
        assertEquals(
                ns.configurations("transforms"),
                ns.resourceMap(ConfigurationsResource.TransformConfigsResource.class));
    }

    public void testResults() {
        assertNotNull(Namespace.get(NS).results());
    }

    public void testToString() {
        assertEquals(NS, Namespace.get(NS).toString());
    }

    public void testValidator() {
        assertNotNull(Namespace.get(NS).validator(DocumentsResource.class));
    }
}
