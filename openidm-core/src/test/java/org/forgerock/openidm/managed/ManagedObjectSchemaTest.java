/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import javax.script.ScriptException;
import java.util.List;
import java.util.Set;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.script.ScriptRegistry;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Tests for {@link ManagedObjectSchema}.
 */
public class ManagedObjectSchemaTest {

    private ManagedObjectSchema schema;

    @BeforeTest
    public void setup() throws JsonValueException, ScriptException {
        ScriptRegistry scriptRegistry = mock(ScriptRegistry.class);
        CryptoService cryptoService = mock(CryptoService.class);
        schema = new ManagedObjectSchema(
                json(object(field("properties", object(
                        field("field1", object(
                                field("type", "string"))),
                        field("field2", object(
                                field("type", "boolean"))),
                        field("field3", object(
                                field("type", "string"),
                                field("isVirtual", true))),
                        field("field4", object(
                                field("type", "string"),
                                field("isVirtual", true),
                                field("returnByDefault", true))),
                        field("field5", object(
                                field("type", "array"),
                                field("items", object(
                                        field("type", "relationship"),
                                        field("properties", object(
                                                field("_ref", object(
                                                        field("type", "string"))))))))),
                        field("field6", object(
                                field("type", "relationship"),
                                field("returnByDefault", true),
                                field("properties", object(
                                        field("_ref", object(
                                                field("type", "string")))))))))))
                , scriptRegistry
                , cryptoService);
    }
    
    @Test
    public void testSchemaFields() {
        Set<JsonPointer> schemaFields = schema.getFields().keySet();
        assertEquals(schemaFields.size(), 6);
        assertTrue(schemaFields.contains(new JsonPointer("field1")));
        assertTrue(schemaFields.contains(new JsonPointer("field2")));
        assertTrue(schemaFields.contains(new JsonPointer("field3")));
        assertTrue(schemaFields.contains(new JsonPointer("field4")));
        assertTrue(schemaFields.contains(new JsonPointer("field5")));
        assertTrue(schemaFields.contains(new JsonPointer("field6")));
    }
    
    @Test
    public void testHiddenByDefaultFields() {
        Set<JsonPointer> hiddenByDefaultFields = schema.getHiddenByDefaultFields().keySet();
        assertEquals(hiddenByDefaultFields.size(), 2);
        assertTrue(!hiddenByDefaultFields.contains(new JsonPointer("field1")));
        assertTrue(!hiddenByDefaultFields.contains(new JsonPointer("field2")));
        assertTrue(hiddenByDefaultFields.contains(new JsonPointer("field3")));
        assertTrue(!hiddenByDefaultFields.contains(new JsonPointer("field4")));
        assertTrue(hiddenByDefaultFields.contains(new JsonPointer("field5")));
        assertTrue(!hiddenByDefaultFields.contains(new JsonPointer("field6")));
    }
    
    @Test
    public void testRelationshipFields() {
        List<JsonPointer> relationshipFields = schema.getRelationshipFields();
        assertEquals(relationshipFields.size(), 2);
        assertTrue(!relationshipFields.contains(new JsonPointer("field1")));
        assertTrue(!relationshipFields.contains(new JsonPointer("field2")));
        assertTrue(!relationshipFields.contains(new JsonPointer("field3")));
        assertTrue(!relationshipFields.contains(new JsonPointer("field4")));
        assertTrue(relationshipFields.contains(new JsonPointer("field5")));
        assertTrue(relationshipFields.contains(new JsonPointer("field6")));
    }
    
    @Test 
    public void testResourceExpansionFields() {
        assertNull(schema.getResourceExpansionField(new JsonPointer("field1")));
        assertNull(schema.getResourceExpansionField(new JsonPointer("field1/*")));
        assertNull(schema.getResourceExpansionField(new JsonPointer("field1/*/*")));
        assertNull(schema.getResourceExpansionField(new JsonPointer("field1/*/field2")));
        assertNull(schema.getResourceExpansionField(SchemaField.FIELD_ALL));
        assertNull(schema.getResourceExpansionField(new JsonPointer("field5/field1")));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field5/*")).getFirst(), new JsonPointer("field5"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field5/*")).getSecond(), SchemaField.FIELD_ALL);
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field5/*/field2")).getFirst(), new JsonPointer("field5"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field5/*/field2")).getSecond(), new JsonPointer("field2"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field5/*/field2/field3")).getFirst(), new JsonPointer("field5"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field5/*/field2/field3")).getSecond(), new JsonPointer("field2/field3"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field6/*")).getFirst(), new JsonPointer("field6"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field6/*")).getSecond(), SchemaField.FIELD_ALL);
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field6/field2")).getFirst(), new JsonPointer("field6"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field6/field2")).getSecond(), new JsonPointer("field2"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field6/field2/field3")).getFirst(), new JsonPointer("field6"));
        assertEquals(schema.getResourceExpansionField(new JsonPointer("field6/field2/field3")).getSecond(), new JsonPointer("field2/field3"));     
    }

    @Test
    public void testHasArrayIndexedField() {
        assertTrue(schema.hasArrayIndexedField(new JsonPointer("field5/0")));
        assertFalse(schema.hasArrayIndexedField(new JsonPointer("field5/*/field2")));
        assertFalse(schema.hasArrayIndexedField(new JsonPointer("field5")));
        assertFalse(schema.hasArrayIndexedField(new JsonPointer("field5/0/field2")));
    }

}
