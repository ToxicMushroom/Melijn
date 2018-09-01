package me.melijn.jda.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TableBuilder {


    private ArrayList<String> headerRow = new ArrayList<>();
    private HashMap<Integer, ArrayList<String>> valueRows = new HashMap<>();
    private HashMap<Integer, Integer> columnWidth = new HashMap<>();
    private ArrayList<String> footerRow = new ArrayList<>();
    boolean split = false;

    public TableBuilder() {

    }

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
        for (String s : footerNames) {
            if (columnWidth.getOrDefault(startColumn, 0) < s.length()) {
                columnWidth.put(startColumn, s.length());
            }
            startColumn++;
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
        String spaces = "                                                                     ";
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
            sb.append(" ").append(value).append(spaces, 0, columnWidth.get(nums++) - value.length()).append(" ║");
        }
        sb.append("\n");

        //third
        sb.append("╠═").append(lijn, 0, columnWidth.get(0));
        for (int i = 1; i < headerRow.size(); i++) {
            sb.append("═╬═").append(lijn, 0, columnWidth.get(i));
        }
        sb.append("═╣\n");

        //main
        for (ArrayList<String> array : valueRows.values()) {
            int numm = 0;
            if (split)
                if (sb.toString().length() + maxRowWidth > 1997 - (footerRow.size() > 0 ? maxRowWidth * 3 : maxRowWidth)) {
                    toReturn.add(sb.toString() + "```");
                    sb = new StringBuilder();
                }
            sb.append("║");
            for (String value : array) {
                sb.append(" ").append(value).append(spaces, 0, columnWidth.get(numm++) - value.length()).append(" ║");
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
                sb.append(" ").append(value).append(spaces, 0, columnWidth.get(nume++) - value.length()).append(" ║");
            }
            sb.append("\n");
        }

        sb.append("╚═").append(lijn, 0, columnWidth.get(0));
        for (int i = 1; i < headerRow.size(); i++) {
            sb.append("═╩═").append(lijn, 0, columnWidth.get(i));
        }
        sb.append("═╝");

        toReturn.add(sb.toString() + "```");
        return toReturn;
    }

}
