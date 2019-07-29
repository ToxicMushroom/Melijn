package me.melijn.melijnbot.objects.utils

import java.util.*
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.set
import kotlin.collections.withIndex


class TableBuilder(private val split: Boolean) {

    private var headerRow: MutableList<String> = ArrayList()
    private var valueRows: MutableMap<Int, List<String>> = HashMap()
    private var columnWidth: MutableMap<Int, Int> = HashMap()
    private var footerRow: MutableList<String> = ArrayList()

    companion object {
        const val LINE = "══════════════════════════════════════════════════════════════════════════════════"
    }

    fun setColumns(headerNames: Collection<String>): TableBuilder {
        headerRow.addAll(headerNames)
        findWidest(headerNames)
        return this
    }

    fun addRow(rowValues: Collection<String>): TableBuilder {
        valueRows[valueRows.size] = ArrayList(rowValues)
        findWidest(rowValues)
        return this
    }

    fun setFooterRow(footerNames: Collection<String>): TableBuilder {
        footerRow.addAll(footerNames)
        findWidest(footerNames)
        return this
    }

    private fun findWidest(footerNames: Collection<String>) {
        for ((temp, s) in footerNames.withIndex()) {
            if (columnWidth.getOrDefault(temp, 0) < s.length) {
                columnWidth[temp] = s.length
            }
        }
    }

    fun build(): List<String> {
        if (valueRows.values.stream().anyMatch { array -> array.size > headerRow.size }) {
            throw IllegalArgumentException("A value row cannot have more values then the header (you can make empty header slots)")
        }

        if (footerRow.size > headerRow.size) {
            throw IllegalArgumentException("A footer row cannot have more values then the header (you can make empty header slots)")
        }


        var maxRowWidth = columnWidth.size * 3 - 2
        for (i in columnWidth.values) {
            maxRowWidth += i
        }


        var sb = StringBuilder()
        val toReturn = ArrayList<String>()
        addTop(sb)
        addRow(sb, headerRow)
        addSplicer(sb)

        //main
        for (i in 0 until valueRows.size) {
            if (split && sb.length + maxRowWidth > 1997 - (if (footerRow.size > 0) maxRowWidth * 3 else maxRowWidth)) {
                toReturn.add("$sb```")
                sb = StringBuilder()
                sb.append("```prolog\n")
            }

            valueRows[i]?.let {
                addRow(sb, it)
            }
        }

        if (footerRow.size > 0) {
            addSplicer(sb)
            addRow(sb, footerRow)
        }
        addBottom(sb)

        toReturn.add("$sb```")

        //less gc
        headerRow.clear()
        valueRows.clear()
        columnWidth.clear()
        footerRow.clear()
        return toReturn
    }

    //╔════╦════╗
    private fun addTop(sb: StringBuilder) {
        sb.append("```prolog\n╔═").append(LINE, 0, columnWidth[0])
        for (i in 1 until headerRow.size) {
            sb.append("═╦═").append(LINE, 0, columnWidth[i])
        }
        sb.append("═╗\n")
    }

    //║    ║    ║
    private fun addRow(sb: StringBuilder, list: List<String>) {
        var nume = 0
        sb.append("║")
        for (value in list) {
            sb.append(" ").append(value).append(" ".repeat(50), 0, columnWidth[nume++]?.minus(value.length)).append(" ║")
        }
        sb.append("\n")
    }


    //╠════╬════╣
    private fun addSplicer(sb: StringBuilder) {
        sb.append("╠═").append(LINE, 0, columnWidth[0])
        for (i in 1 until footerRow.size) {
            sb.append("═╬═").append(LINE, 0, columnWidth[i])
        }
        sb.append("═╣\n")
    }

    //╚════╩════╝
    private fun addBottom(sb: StringBuilder) {
        sb.append("╚═").append(LINE, 0, columnWidth[0])
        for (i in 1 until headerRow.size) {
            sb.append("═╩═").append(LINE, 0, columnWidth[i])
        }
        sb.append("═╝")
    }
}