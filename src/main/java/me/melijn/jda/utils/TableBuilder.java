package me.melijn.jda.utils;

import java.util.*;

public class TableBuilder {


    private List<String> headerRow = new ArrayList<>();
    private Map<Integer, List<String>> valueRows = new HashMap<>();
    private Map<Integer, Integer> columnWidth = new HashMap<>();
    private List<String> footerRow = new ArrayList<>();
    private boolean split;

    public TableBuilder(boolean shouldSplit) {
        this.split = shouldSplit;
    }

    public TableBuilder setColumns(Collection<String> headerNames) {
        headerRow.addAll(headerNames);
        findWidest(headerNames, 0);
        return this;
    }

    public TableBuilder addRow(Collection<String> rowValues) {
        valueRows.put(valueRows.size(), new ArrayList<>(rowValues));
        findWidest(rowValues, 0);
        return this;
    }

    public TableBuilder setFooterRow(Collection<String> footerNames) {
        footerRow.addAll(footerNames);
        findWidest(footerNames, 0);
        return this;
    }

    private void findWidest(Collection<String> footerNames, int startColumn) {
        int temp = startColumn;
        for (String s : footerNames) {
            if ((columnWidth.getOrDefault(temp, 0)) < s.length()) {
                columnWidth.put(temp, s.length());
            }
            temp++;
        }
    }

    public List<String> build() {
        if (valueRows.values().stream().anyMatch(array -> array.size() > headerRow.size())) {
            throw new IllegalArgumentException("A value row cannot have more values then the header (you can make empty header slots)");
        }

        if (footerRow.size() > headerRow.size()) {
            throw new IllegalArgumentException("A footer row cannot have more values then the header (you can make empty header slots)");
        }


        int maxRowWidth = (columnWidth.size() * 3) - 2;
        for (int i : columnWidth.values()) {
            maxRowWidth += i;
        }

        String lijn = "══════════════════════════════════════════════════════════════════════════════════";
        StringBuilder sb = new StringBuilder();
        List<String> toReturn = new ArrayList<>();
        //firstline
        sb.append("```prolog\n╔═").append(lijn, 0, columnWidth.get(0));
        for (int i = 1; i < headerRow.size(); i++) {
            sb.append("═╦═").append(lijn, 0, columnWidth.get(i));
        }
        sb.append("═╗\n");

        //second
        int nums = 0;
        sb.append("║");
        for (String value : headerRow) {
            sb.append(" ").append(value).append(" ".repeat(50), 0, columnWidth.get(nums++) - value.length()).append(" ║");
        }
        sb.append("\n");

        //third
        sb.append("╠═").append(lijn, 0, columnWidth.get(0));
        for (int i = 1; i < headerRow.size(); i++) {
            sb.append("═╬═").append(lijn, 0, columnWidth.get(i));
        }
        sb.append("═╣\n");

        //main
        for (int i = 0; i < valueRows.size(); i++) {
            int numm = 0;
            if (split && sb.length() + maxRowWidth > 1997 - (footerRow.size() > 0 ? maxRowWidth * 3 : maxRowWidth)) {
                toReturn.add(sb.toString() + "```");
                sb = new StringBuilder();
                sb.append("```prolog\n");
            }
            sb.append("║");
            for (String value : valueRows.get(i)) {
                sb.append(" ").append(value).append(" ".repeat(50), 0, columnWidth.get(numm++) - value.length()).append(" ║");
            }
            sb.append("\n");
        }

        //possible end
        if (footerRow.size() > 0) {
            sb.append("╠═").append(lijn, 0, columnWidth.get(0));
            for (int i = 1; i < footerRow.size(); i++) {
                sb.append("═╬═").append(lijn, 0, columnWidth.get(i));
            }
            sb.append("═╣\n");

            int nume = 0;
            sb.append("║");
            for (String value : footerRow) {
                sb.append(" ").append(value).append(" ".repeat(50), 0, columnWidth.get(nume++) - value.length()).append(" ║");
            }
            sb.append("\n");
        }

        sb.append("╚═").append(lijn, 0, columnWidth.get(0));
        for (int i = 1; i < headerRow.size(); i++) {
            sb.append("═╩═").append(lijn, 0, columnWidth.get(i));
        }
        sb.append("═╝");

        toReturn.add(sb.toString() + "```");

        //less gc
        headerRow = null;
        valueRows = null;
        columnWidth = null;
        footerRow = null;
        return toReturn;
    }

}
