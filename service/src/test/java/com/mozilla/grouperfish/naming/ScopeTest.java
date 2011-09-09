package com.mozilla.grouperfish.naming;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.mozilla.grouperfish.model.Access;
import com.mozilla.grouperfish.model.Type;
import com.mozilla.grouperfish.model.DummyAccess;
import com.mozilla.grouperfish.model.Access.Operation;
import com.mozilla.grouperfish.naming.Scope;
import com.mozilla.grouperfish.rest.jaxrs.ConfigurationsResource;
import com.mozilla.grouperfish.rest.jaxrs.DocumentsResource;
import com.mozilla.grouperfish.rest.jaxrs.QueriesResource;
import com.mozilla.grouperfish.rest.jaxrs.ResultsResource;
import com.mozilla.grouperfish.services.api.Grid;
import com.mozilla.grouperfish.services.mock.MockGrid;


@Test(groups="unit")
public class ScopeTest {

    private final String NS = "unit-test";
    private final Grid grid = new MockGrid();
    private final Access DUMMY_ACCESS = new DummyAccess(Operation.CREATE, "dummy.example.com");

    public void testAllows() {
        assertTrue(scope(NS).allows(DocumentsResource.class, DUMMY_ACCESS));
    }

    public void testExistingConfigurations() {
        for (final Type type : Type.values()) {
            assertNotNull(scope(NS).map(type));
        }
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testInvalidConfigurations() {
        scope(NS).map(null);
    }

    public void testDocuments() {
        assertNotNull(scope(NS).documents());
    }

    public void testMaxLength() {
        Access access = new DummyAccess(Operation.CREATE, "dummy.example.com");
        assertTrue(0 < scope(NS).maxLength(DocumentsResource.class, access));
    }

    public void testQueries() {
        assertNotNull(scope(NS).queries());
    }

    public void testResourceMap() {
        Scope ns = scope(NS);
        assertEquals(
                ns.documents(), ns.resourceMap(DocumentsResource.class));
        assertEquals(
                ns.queries(), ns.resourceMap(QueriesResource.class));
        assertEquals(
                ns.results(), ns.resourceMap(ResultsResource.class));
        assertEquals(
                ns.map(Type.CONFIGURATION_FILTER),
                ns.resourceMap(ConfigurationsResource.FilterConfigsResource.class));
        assertEquals(
                ns.map(Type.CONFIGURATION_TRANSFORM),
                ns.resourceMap(ConfigurationsResource.TransformConfigsResource.class));
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void testInvalidResourceMap() {
        final Scope ns = scope(NS);
        ns.resourceMap(Object.class);
    }

    public void testResults() {
        assertNotNull(scope(NS).results());
    }

    public void testToString() {
        assertEquals(NS, scope(NS).raw());
    }

    public void testValidator() {
        assertNotNull(scope(NS).validator(DocumentsResource.class));
    }

    private Scope scope(String namespace) {
        return new Scope(namespace, grid);
    }
}
