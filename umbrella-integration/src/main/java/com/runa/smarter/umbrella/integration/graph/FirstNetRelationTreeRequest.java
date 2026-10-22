package com.runa.smarter.umbrella.integration.graph;

import java.util.List;

/** {@code getFirstNetRelationTree} 固定请求体。 */
public record FirstNetRelationTreeRequest(
        boolean includeOrg,
        boolean includePower,
        List<String> includePowerBranchTypes,
        boolean includeStation,
        boolean includeStationBranch,
        List<String> sortRules) {

    public static final FirstNetRelationTreeRequest DEFAULT = new FirstNetRelationTreeRequest(
            false,
            true,
            List.of(
                    "FIRST_STATION",
                    "RELAY_PUMPING_STATION",
                    "PRESSURE_ISOLATION_STATION",
                    "SPUR"),
            true,
            true,
            List.of());

}
