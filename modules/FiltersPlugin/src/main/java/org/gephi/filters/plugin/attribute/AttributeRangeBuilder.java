/*
 Copyright 2008-2010 Gephi
 Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
 Website : http://www.gephi.org

 This file is part of Gephi.

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright 2011 Gephi Consortium. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. You can obtain a copy of the License at
 http://gephi.org/about/legal/license-notice/
 or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 specific language governing permissions and limitations under the
 License.  When distributing the software, include this License Header
 Notice in each file and include the License files at
 /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
 License Header, with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"

 If you wish your version of this file to be governed by only the CDDL
 or only the GPL Version 3, indicate your decision by adding
 "[Contributor] elects to include this software in this distribution
 under the [CDDL or GPL Version 3] license." If you do not indicate a
 single choice of license, a recipient has the option to distribute
 your version of this file under either the CDDL, the GPL Version 3 or
 to extend the choice of license to its licensees as provided above.
 However, if you add GPL Version 3 code and therefore, elected the GPL
 Version 3 license, then the option applies only if the new code is
 made subject to such option by the copyright holder.

 Contributor(s):

 Portions Copyrighted 2011 Gephi Consortium.
 */
package org.gephi.filters.plugin.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeUtils;
import org.gephi.filters.api.FilterLibrary;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.AbstractAttributeFilter;
import org.gephi.filters.plugin.AbstractAttributeFilterBuilder;
import org.gephi.filters.plugin.DynamicAttributesHelper;
import org.gephi.filters.plugin.graph.RangeUI;
import org.gephi.filters.spi.*;
import org.gephi.graph.api.*;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Mathieu Bastian
 */
@ServiceProvider(service = CategoryBuilder.class)
public class AttributeRangeBuilder implements CategoryBuilder {

    private final static Category RANGE = new Category(
            NbBundle.getMessage(AttributeRangeBuilder.class, "AttributeRangeBuilder.name"),
            null,
            FilterLibrary.ATTRIBUTES);

    public Category getCategory() {
        return RANGE;
    }

    public FilterBuilder[] getBuilders() {
        List<FilterBuilder> builders = new ArrayList<FilterBuilder>();
        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
        List<AttributeColumn> columns = new ArrayList<AttributeColumn>();
        columns.addAll(Arrays.asList(am.getNodeTable().getColumns()));
        columns.addAll(Arrays.asList(am.getEdgeTable().getColumns()));
        for (AttributeColumn c : columns) {
            if (AttributeUtils.getDefault().isNumberColumn(c) || AttributeUtils.getDefault().isDynamicNumberColumn(c)) {
                AttributeRangeFilterBuilder b = new AttributeRangeFilterBuilder(c);
                builders.add(b);
            }
        }
        return builders.toArray(new FilterBuilder[0]);
    }

    private static class AttributeRangeFilterBuilder extends AbstractAttributeFilterBuilder {

        public AttributeRangeFilterBuilder(AttributeColumn column) {
            super(column,
                    RANGE,
                    NbBundle.getMessage(AttributeEqualBuilder.class, "AttributeRangeBuilder.description"),
                    null);
        }

        public AttributeRangeFilter getFilter() {
            return new AttributeRangeFilter(column);
        }

        public JPanel getPanel(Filter filter) {
            RangeUI ui = Lookup.getDefault().lookup(RangeUI.class);
            if (ui != null) {
                return ui.getPanel((AttributeRangeFilter) filter);
            }
            return null;
        }
    }

    public static class AttributeRangeFilter extends AbstractAttributeFilter implements RangeFilter {

        private Range range;
        private DynamicAttributesHelper dynamicHelper = new DynamicAttributesHelper(this, null);

        public AttributeRangeFilter(AttributeColumn column) {
            super(NbBundle.getMessage(AttributeRangeBuilder.class, "AttributeRangeBuilder.name"),
                    column);

            //Add property
            addProperty(Range.class, "range");
        }

        public boolean init(Graph graph) {
            HierarchicalGraph hg = (HierarchicalGraph) graph;
            if (AttributeUtils.getDefault().isNodeColumn(column)) {
                if (graph.getNodeCount() == 0) {
                    return false;
                }
            } else if (AttributeUtils.getDefault().isEdgeColumn(column)) {
                if (hg.getTotalEdgeCount() == 0) {
                    return false;
                }
            }
            dynamicHelper = new DynamicAttributesHelper(this, hg);
            return true;
        }

        public boolean evaluate(Graph graph, Attributable attributable) {
            Object val = attributable.getAttributes().getValue(column.getIndex());
            val = dynamicHelper.getDynamicValue(val);
            if (val != null) {
                return range.isInRange((Number) val);
            }
            return false;

        }

        public void finish() {
        }

        public Number[] getValues(Graph graph) {
            List<Number> vals = new ArrayList<Number>();
            if (AttributeUtils.getDefault().isNodeColumn(column)) {
                for (Node n : graph.getNodes()) {
                    Object val = n.getNodeData().getAttributes().getValue(column.getIndex());
                    val = dynamicHelper.getDynamicValue(val);
                    if (val != null) {
                        vals.add((Number) val);
                    }
                }
            } else {
                for (Edge e : ((HierarchicalGraph) graph).getEdgesAndMetaEdges()) {
                    Object val = e.getEdgeData().getAttributes().getValue(column.getIndex());
                    val = dynamicHelper.getDynamicValue(val);
                    if (val != null) {
                        vals.add((Number) val);
                    }
                }
            }
            return vals.toArray(new Number[0]);
        }

        public FilterProperty getRangeProperty() {
            return getProperties()[1];
        }

        public Range getRange() {
            return range;
        }

        public void setRange(Range range) {
            this.range = range;
        }
    }
}
