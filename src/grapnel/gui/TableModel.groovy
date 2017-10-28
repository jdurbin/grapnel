package grapnel.gui;

import groovy.swing.SwingBuilder
import javax.swing.JTable
import javax.swing.*
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout as BL
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import grapnel.util.*

import com.google.common.primitives.Doubles;

//*************************************
// TableModel 
// 
// Back a swing.JTable with a grapnel.Table
// 
//*************************************
class TableModel extends AbstractTableModel{		
	Table dt;
	
	// As the user selects columns, determine if the 
	// column is numeric or not and cache this fact 
	// for sorting and such.  
	def column2TypeCache = [:]
	
	TableModel(Table table){dt = table;}

	public String getColumnName(int col) {return(dt.colNames[col]);}	
	public String getRowName(int row){
		def rowName = dt.rowNames[row]
		println "getRowName: $row \t $rowName"
		return(rowName);
	}	
	public int getRowCount() { return(dt.rows())}
	public int getColumnCount() { return(dt.cols())}
	
	// Maybe implement this in Table as a special 
	// getTypedAt() sort of method... 	
	// Do type conversion here instead of loading because 
	// don't want to pay to convert everything, just what see/use. 
	public Object getValueAt(int row, int col) {		
		if (col==0) return(dt.get(row,col))							
		if (column2TypeCache.containsKey(col)){
			if(column2TypeCache[col] == Double.class){
				try{
				return(Double.parseDouble(dt.get(row,col)))
				}catch(Exception e){
					return(Double.NaN)
				}
			}else{
				return(dt.get(row,col))	
			}
		}else{
			column2TypeCache[col] = determineColumnType(col)
			if(column2TypeCache[col] == Double.class){
				try{
					return(Double.parseDouble(dt.get(row,col)))
				}catch(Exception e){
					return(Double.NaN)
				}
			}else{
				return(dt.get(row,col))
			}
		}
	}
	
	public Class getColumnClass(int c) {
		if (c == 0) return(String.class)
		
		if (column2TypeCache.containsKey(c)){
			return(column2TypeCache[c])
		}else{
			column2TypeCache[c] = determineColumnType(c)
			return(column2TypeCache[c])
		}
	}  	
	public boolean isCellEditable(int row, int col){return false;}	
	public void setValueAt(Object value, int row, int col) {}
	
	
	// To make it more efficient only look at the first 100 rows
	// If they are all numbers, assume the whole column is numbers. 
	def determineColumnType(c){
		def rows = dt.numRows 
		int scanEnd = (500 < rows) ? 500 : (rows -1)
				
		int numcount = 0;
		for(int i = 0;i < scanEnd;i++){
			 if (Doubles.tryParse(dt.get(i,c)) != null) numcount++
		}
		def acceptCount = scanEnd* 0.5
		if (numcount >= acceptCount) {
			//System.err.println "col: ${getColumnName(c)} is Double"
			return(Double.class)
		}
		else {
			//System.err.println "col: ${getColumnName(c)} is String"
			return(String.class)		
		}
	} 		
}
