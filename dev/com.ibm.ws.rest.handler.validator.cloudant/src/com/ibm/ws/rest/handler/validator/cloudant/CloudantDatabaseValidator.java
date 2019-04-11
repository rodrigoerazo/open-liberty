/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.cloudant;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.validator.Validator;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { Validator.class },
           property = { "service.vendor=IBM", "com.ibm.wsspi.rest.handler.root=/validator", "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.cloudant.cloudantDatabase" })
public class CloudantDatabaseValidator implements Validator {
    private final static TraceComponent tc = Tr.register(CloudantDatabaseValidator.class);

    @Reference
    private ResourceConfigFactory resourceConfigFactory;

    /**
     * @see com.ibm.wsspi.validator.Validator#validate(java.lang.Object, java.util.Map, java.util.Locale)
     */
    @Override
    public LinkedHashMap<String, ?> validate(Object instance, Map<String, Object> props, Locale locale) {
        String auth = (String) props.get("auth");
        String authAlias = (String) props.get("authAlias");

        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "validate", auth, authAlias);

        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();

        try {
            ResourceConfig config = null;
            int authType = "container".equals(auth) ? 0 : "application".equals(auth) ? 1 : -1;
            if (authType >= 0) {
                config = resourceConfigFactory.createResourceConfig("com.cloudant.client.api.Database");
                config.setResAuthType(authType);
                if (authAlias != null && authType == 0)
                    config.addLoginProperty("DefaultPrincipalMapping", authAlias); // set provided auth alias
            }

            Object database = ((ResourceFactory) instance).createResource(config);
            //There isn't anything particularly useful in the DB Info, but invoking the method
            //ensures that the database exists (or it will be created if create="true")
            database.getClass().getMethod("info").invoke(database);

            URI dbURI = (URI) database.getClass().getMethod("getDBUri").invoke(database);
            result.put("uri", dbURI == null ? "null" : dbURI.toString());
        } catch (Throwable x) {
            result.put("failure", x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "validate", result);
        return result;
    }
}