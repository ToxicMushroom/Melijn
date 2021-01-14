package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.enums.Alignment
import me.melijn.melijnbot.internals.models.Cell
import kotlin.math.ceil
import kotlin.math.floor


class TableBuilder {

    private val headerRow = mutableListOf<Cell>()
    private val valueRows = mutableMapOf<Int, List<Cell>>()
    private val footerRow = mutableListOf<Cell>()
    private val columnWidth = mutableMapOf<Int, Int>()
    private val extraSplit = mutableMapOf<Int, String>()

    var defaultSeperator = " | "
    var seperatorOverrides = mutableMapOf<Int, String>()

    var codeBlockLanguage = "markdown"

    var headerSeperator = "="
    var footerTopSeperator = "~"
    var footerBottomSeperator = ""
    var rowPrefix = ""
    var rowSuffix = ""


    fun addSplit(character: String = footerTopSeperator) {
        extraSplit[valueRows.size] = character
    }

    fun setColumns(vararg headerValues: String): TableBuilder {
        val cells = headerValues.map { Cell(it) }.toTypedArray()

        headerRow.addAll(cells)
        findWidest(*cells)
        return this
    }

    fun setColumns(vararg headerCells: Cell): TableBuilder {
        headerRow.addAll(headerCells)
        findWidest(*headerCells)
        return this
    }

    fun addRow(vararg rowElements: String): TableBuilder {
        val cellList = rowElements.map { Cell(it) }
        valueRows[valueRows.size] = cellList
        findWidest(*cellList.toTypedArray())
        return this
    }

    fun addRow(vararg rowCells: Cell): TableBuilder {
        valueRows[valueRows.size] = rowCells.toList()
        findWidest(*rowCells)
        return this
    }

    fun setFooterRow(vararg footerElements: String): TableBuilder {
        val cells = footerElements.map { Cell(it) }.toTypedArray()

        footerRow.addAll(cells)
        findWidest(*cells)
        return this
    }

    fun setFooterRow(vararg footerElements: Cell): TableBuilder {
        footerRow.addAll(footerElements)
        findWidest(*footerElements)
        return this
    }

    private fun findWidest(vararg rowElements: Cell) {
        for ((temp, s) in rowElements.withIndex()) {
            if (columnWidth.getOrDefault(temp, 0) < s.value.length) {
                columnWidth[temp] = s.value.length
            }
        }
    }

    fun build(split: Boolean): List<String> {
        require(!valueRows.values.stream().anyMatch { array -> array.size > headerRow.size }) {
            "A value row cannot have more values then the header (you can make empty header slots)"
        }

        require(footerRow.size <= headerRow.size) {
            "A footer row cannot have more values then the header (you can make empty header slots)"
        }


        var maxRowWidth = columnWidth.size * 3 - 2
        for (i in columnWidth.values) {
            maxRowWidth += i
        }


        var sb = StringBuilder()
        val toReturn = ArrayList<String>()
        addCodeblockStart(sb)
        addRow(sb, headerRow)
        addHeaderSplicer(sb)

        //main
        for ((index, element) in valueRows) {
            if (extraSplit.containsKey(index)) {
                extraSplit[index]?.let { addSplicer(sb, it, headerRow) }
            }

            if (split && sb.length + maxRowWidth > 1997 - (if (footerRow.size > 0) maxRowWidth * 3 else maxRowWidth)) {
                toReturn.add("$sb```")
                sb = StringBuilder()
                sb.append("```$codeBlockLanguage\n")
            }

            addRow(sb, element)
        }

        if (footerRow.size > 0) {
            if (footerTopSeperator.isNotEmpty()) {
                addTopFooterSplicer(sb)
            }
            addRow(sb, footerRow)
        }
        if (footerBottomSeperator.isNotEmpty()) {
            addBottom(sb)
        }

        toReturn.add("$sb```")

        // less gc
        headerRow.clear()
        valueRows.clear()
        columnWidth.clear()
        footerRow.clear()
        return toReturn
    }

    // CodeBlockStart
    private fun addCodeblockStart(sb: StringBuilder) {
        sb.append("```$codeBlockLanguage\n")
    }

    private fun addRow(sb: StringBuilder, list: List<Cell>) {
        sb.append(rowPrefix)
        for ((i, cell) in list.withIndex()) {
            when (cell.alignment) {
                Alignment.LEFT -> {
                    sb
                        .append(cell.value)

                    if (i != list.size - 1 || rowSuffix.isNotEmpty()) {
                        sb.append(getSpaces(i, cell.value))
                    }
                }
                Alignment.RIGHT -> {
                    sb
                        .append(getSpaces(i, cell.value))
                        .append(cell.value)
                }
                Alignment.CENTER -> {
                    sb
                        .append(getLeftSpaces(i, cell.value))
                        .append(cell.value)

                    if (i != list.size - 1 || rowSuffix.isNotEmpty()) {
                        sb.append(getRightSpaces(i, cell.value))
                    }
                }
            }
            if (i != list.size - 1) {
                sb.append(seperatorOverrides.getOrDefault(i, defaultSeperator))
            }
        }
        sb
            .append(rowSuffix)
            .append("\n")
    }


    private fun addHeaderSplicer(sb: StringBuilder) {
        addSplicer(sb, headerSeperator, headerRow)
    }

    private fun addTopFooterSplicer(sb: StringBuilder) {
        addSplicer(sb, footerTopSeperator, footerRow)
    }

    private fun addBottom(sb: StringBuilder) {
        addSplicer(sb, footerBottomSeperator, footerRow)
    }

    private fun addSplicer(sb: StringBuilder, separator: String, row: List<Cell>) {
        for (i in row.indices) {
            val separatorLength = if (i != row.size - 1) {
                seperatorOverrides.getOrDefault(i, defaultSeperator).length + if (i == 0) {
                    rowPrefix.length
                } else {
                    0
                }
            } else {
                rowSuffix.length
            }
            val length = columnWidth[i]?.plus(separatorLength) ?: throw IllegalArgumentException("error")
            sb.append(separator.repeat(length))
        }

        sb.append("\n")
    }

    private fun getSpaces(widthIndex: Int, value: String): String {
        return columnWidth[widthIndex]?.minus(value.length)?.let {
            " ".repeat(50).substring(0, it)
        } ?: ""
    }

    private fun getLeftSpaces(widthIndex: Int, value: String): String {
        return columnWidth[widthIndex]?.minus(value.length)?.let {
            " ".repeat(50).substring(0, floor(it / 2.0).toInt())
        } ?: ""
    }

    private fun getRightSpaces(widthIndex: Int, value: String): String {
        return columnWidth[widthIndex]?.minus(value.length)?.let {
            " ".repeat(50).substring(0, ceil(it / 2.0).toInt())
        } ?: ""
    }
}