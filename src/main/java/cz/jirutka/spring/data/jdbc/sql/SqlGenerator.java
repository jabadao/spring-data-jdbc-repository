/*
 * Copyright 2012-2014 Tomasz Nurkiewicz <nurkiewicz@gmail.com>.
 * Copyright 2016 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.jirutka.spring.data.jdbc.sql;

import cz.jirutka.spring.data.jdbc.TableDescription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static cz.jirutka.spring.data.jdbc.internal.StringUtils.repeat;
import static java.lang.String.format;
import static org.springframework.util.StringUtils.collectionToDelimitedString;

public class SqlGenerator {

    static final String
            AND = " AND ",
            OR = " OR ",
            COMMA = ", ",
            PARAM = " = ?";


    public String count(TableDescription table) {
        return format("SELECT count(*) FROM %s", table.getFromClause());
    }

    public String deleteById(TableDescription table) {
        return format("DELETE FROM %s WHERE %s", table.getTableName(), idPredicate(table));
    }

    public String selectAll(TableDescription table) {
        return format("SELECT %s FROM %s", table.getSelectClause(), table.getFromClause());
    }

    public String selectAll(TableDescription table, Pageable page) {
        return selectAll(table, page.getSort()) + limitClause(page);
    }

    public String selectAll(TableDescription table, Sort sort) {
        return selectAll(table) + (sort != null ? orderByClause(sort) : "");
    }

    public String selectById(TableDescription table) {
        return selectByIds(table, 1);
    }

    public String selectByIds(TableDescription table, int idsCount) {
        return idsCount > 0
            ? selectAll(table) + " WHERE " + idsPredicate(table, idsCount)
            : selectAll(table);
    }

    public String update(TableDescription table, Map<String, Object> columns) {

        return format("UPDATE %s SET %s WHERE %s",
            table.getTableName(),
            formatParameters(columns.keySet(), COMMA),
            idPredicate(table));
    }

    public String insert(TableDescription table, Map<String, Object> columns) {

        return format("INSERT INTO %s (%s) VALUES (%s)",
            table.getTableName(),
            collectionToDelimitedString(columns.keySet(), COMMA),
            repeat("?", COMMA, columns.size()));
    }

    public String deleteAll(TableDescription table) {
        return format("DELETE FROM %s", table.getTableName());
    }

    public String existsById(TableDescription table) {
        return format("SELECT 1 FROM %s WHERE %s", table.getTableName(), idPredicate(table));
    }


    protected String limitClause(Pageable page) {
        return format(" LIMIT %d, %d", page.getOffset(), page.getPageSize());
    }

    protected String orderByClause(Sort sort) {
        return " ORDER BY " + orderByExpression(sort);
    }

    protected String orderByExpression(Sort sort) {
        StringBuilder sb = new StringBuilder();

        for (Iterator<Order> it = sort.iterator(); it.hasNext(); ) {
            Order order = it.next();
            sb.append(order.getProperty()).append(' ').append(order.getDirection());

            if (it.hasNext()) sb.append(COMMA);
        }
        return sb.toString();
    }


    private String idPredicate(TableDescription table) {
        return formatParameters(table.getPkColumns(), AND);
    }

    private String idsPredicate(TableDescription table, int idsCount) {
        Assert.isTrue(idsCount > 0, "idsCount must be greater than zero");

        List<String> idColumnNames = table.getPkColumns();

        if (idsCount == 1) {
            return idPredicate(table);

        } else if (idColumnNames.size() > 1) {
            return repeat("(" + formatParameters(idColumnNames, AND) + ")", OR, idsCount);

        } else {
            return idColumnNames.get(0) + " IN (" + repeat("?", COMMA, idsCount) + ")";
        }
    }

    private String formatParameters(Collection<String> columns, String delimiter) {
        return collectionToDelimitedString(columns, delimiter, "", PARAM);
    }
}