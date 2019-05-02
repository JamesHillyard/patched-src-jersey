/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.microprofile.restclient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.model.Parameter;

/**
 * Model of method parameter annotated by {@link BeanParam} annotation.
 *
 * @author David Kral
 */
class BeanClassModel {

    private final Class<?> beanClass;
    private final List<ParamModel> parameterModels;

    /**
     * Create new instance of bean annotated parameter.
     *
     * @param interfaceModel rest client interface model
     * @param beanClass bean annotated parameter class
     * @return new instance
     */
    static BeanClassModel fromClass(InterfaceModel interfaceModel, Class<?> beanClass) {
        return new Builder(interfaceModel, beanClass)
                .processPathFields()
                .processHeaderFields()
                .processCookieFields()
                .processQueryFields()
                .processMatrixFields()
                .build();
    }

    private BeanClassModel(Builder builder) {
        this.beanClass = builder.beanClass;
        this.parameterModels = builder.parameterModels;
    }

    /**
     * List of all class fields annotated with supported parameter annotation
     *
     * @return parameter model list
     */
    List<ParamModel> getParameterModels() {
        return parameterModels;
    }

    /**
     * Resolves bean path parameters.
     *
     * @param webTarget web target path
     * @param instance actual method parameter value
     * @return updated web target path
     */
    @SuppressWarnings("unchecked")
    WebTarget resolvePath(WebTarget webTarget, Object instance) {
        AtomicReference<WebTarget> toReturn = new AtomicReference<>(webTarget);
        parameterModels.stream()
                .filter(paramModel -> paramModel.handles(PathParam.class))
                .forEach(parameterModel -> {
                    Field field = (Field) parameterModel.getAnnotatedElement();
                    toReturn.set((WebTarget) parameterModel.handleParameter(webTarget,
                                                                            PathParam.class,
                                                                            resolveValueFromField(field, instance)));
                });
        return toReturn.get();
    }

    /**
     * Resolves bean header parameters.
     *
     * @param headers headers
     * @param instance actual method parameter value
     * @return updated headers
     */
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, Object> resolveHeaders(MultivaluedMap<String, Object> headers,
                                                  Object instance) {
        parameterModels.stream()
                .filter(paramModel -> paramModel.handles(HeaderParam.class))
                .forEach(parameterModel -> {
                    Field field = (Field) parameterModel.getAnnotatedElement();
                    parameterModel.handleParameter(headers,
                                                   HeaderParam.class,
                                                   resolveValueFromField(field, instance));
                });
        return headers;
    }

    /**
     * Resolves bean cookie parameters.
     *
     * @param cookies cookies
     * @param instance actual method parameter value
     * @return updated cookies
     */
    @SuppressWarnings("unchecked")
    Map<String, String> resolveCookies(Map<String, String> cookies,
                                       Object instance) {
        parameterModels.stream()
                .filter(paramModel -> paramModel.handles(CookieParam.class))
                .forEach(parameterModel -> {
                    Field field = (Field) parameterModel.getAnnotatedElement();
                    parameterModel.handleParameter(cookies,
                                                   CookieParam.class,
                                                   resolveValueFromField(field, instance));
                });
        return cookies;
    }

    /**
     * Resolves bean query parameters.
     *
     * @param query queries
     * @param instance actual method parameter value
     * @return updated queries
     */
    @SuppressWarnings("unchecked")
    Map<String, Object[]> resolveQuery(Map<String, Object[]> query,
                                              Object instance) {
        parameterModels.stream()
                .filter(paramModel -> paramModel.handles(QueryParam.class))
                .forEach(parameterModel -> {
                    Field field = (Field) parameterModel.getAnnotatedElement();
                    parameterModel.handleParameter(query,
                                                   QueryParam.class,
                                                   resolveValueFromField(field, instance));
                });
        return query;
    }

    /**
     * Resolves bean matrix parameters.
     *
     * @param webTarget web target path
     * @param instance actual method parameter value
     * @return updated web target path
     */
    @SuppressWarnings("unchecked")
    WebTarget resolveMatrix(WebTarget webTarget,
                                       Object instance) {
        AtomicReference<WebTarget> toReturn = new AtomicReference<>(webTarget);
        parameterModels.stream()
                .filter(paramModel -> paramModel.handles(MatrixParam.class))
                .forEach(parameterModel -> {
                    Field field = (Field) parameterModel.getAnnotatedElement();
                    toReturn.set((WebTarget) parameterModel.handleParameter(webTarget,
                                                                            MatrixParam.class,
                                                                            resolveValueFromField(field, instance)));
                });
        return toReturn.get();
    }


    /**
     * Resolves bean form parameters.
     *
     * @param form web form
     * @param instance actual method parameter value
     * @return updated web form
     */
    @SuppressWarnings("unchecked")
    Form resolveForm(Form form,
                          Object instance) {
        parameterModels.stream()
                .filter(paramModel -> paramModel.handles(FormParam.class))
                .forEach(parameterModel -> {
                    Field field = (Field) parameterModel.getAnnotatedElement();
                    parameterModel.handleParameter(form,
                                                   FormParam.class,
                                                   resolveValueFromField(field, instance));
                });
        return form;
    }

    private Object resolveValueFromField(Field field, Object instance) {
        try {
            Object toReturn;
            field.setAccessible(true);
            toReturn = field.get(instance);
            field.setAccessible(false);
            return toReturn;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Builder {

        private final InterfaceModel interfaceModel;
        private final Class<?> beanClass;
        private ArrayList<ParamModel> parameterModels = new ArrayList<>();

        private Builder(InterfaceModel interfaceModel, Class<?> beanClass) {
            this.interfaceModel = interfaceModel;
            this.beanClass = beanClass;
        }

        /**
         * Parses all {@link PathParam} annotated fields from bean class.
         *
         * @return updated builder instance
         */
        Builder processPathFields() {
            return processFieldsByParameterClass(PathParam.class);
        }

        /**
         * Parses all {@link HeaderParam} annotated fields from bean class.
         *
         * @return updated builder instance
         */
        Builder processHeaderFields() {
            return processFieldsByParameterClass(HeaderParam.class);
        }

        /**
         * Parses all {@link CookieParam} annotated fields from bean class.
         *
         * @return updated builder instance
         */
        Builder processCookieFields() {
            return processFieldsByParameterClass(CookieParam.class);
        }

        /**
         * Parses all {@link QueryParam} annotated fields from bean class.
         *
         * @return updated builder instance
         */
        Builder processQueryFields() {
            return processFieldsByParameterClass(QueryParam.class);
        }

        /**
         * Parses all {@link MatrixParam} annotated fields from bean class.
         *
         * @return updated builder instance
         */
        Builder processMatrixFields() {
            return processFieldsByParameterClass(MatrixParam.class);
        }

        private Builder processFieldsByParameterClass(Class<? extends Annotation> parameterClass) {
            for (Field field : beanClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(parameterClass)) {
                    Parameter parameter = Parameter.create(parameterClass, parameterClass, false,
                                                           field.getType(), field.getGenericType(),
                                                           field.getDeclaredAnnotations());
                    parameterModels.add(ParamModel.from(interfaceModel, field.getType(), field,
                                                        parameter, -1));
                }
            }
            return this;
        }

        /**
         * Creates new BeanClassModel instance.
         *
         * @return new instance
         */
        BeanClassModel build() {
            return new BeanClassModel(this);
        }
    }

}
