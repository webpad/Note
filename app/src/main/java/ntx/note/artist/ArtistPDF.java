package ntx.note.artist;

import android.util.Log;

import junit.framework.Assert;
import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.Paper.Type;
import ntx.note.artist.LineStyle.Cap;
import ntx.note.artist.LineStyle.Join;

import org.libharu.Document;
import org.libharu.Document.CompressionMode;
import org.libharu.Font;
import org.libharu.Font.BuiltinFont;
import org.libharu.Image;
import org.libharu.Page;
import org.libharu.Page.LineCap;
import org.libharu.Page.LineJoin;
import org.libharu.Page.PageDirection;
import org.libharu.Page.PageSize;

import java.io.File;
import java.io.IOException;


public class ArtistPDF
	extends Artist {
	
	private final static String TAG = "ArtistPDF";
	
	private final File file;
	private final Document doc;
	private Page pdf = null;
	
	protected float height;
	protected float width;
	protected float offset_x;
	protected float offset_y;

	private Font pageNumberFont;
	protected final int pageNumberSize = 14;
	protected int pageNumber = 1;
	private PageSize pageSize;

	private boolean rotate = false;

	
	public ArtistPDF(File file) {
		this.file = file;
		doc = new Document();
		doc.setCompressionMode(CompressionMode.COMP_ALL);
		pageNumberFont = doc.getFont(BuiltinFont.COURIER_BOLD);
	}

	public void setPaper(PaperType paper) {
		switch (paper.getPageSize()) {
		case LETTER:
			this.pageSize = PageSize.LETTER;
			break;
		case LEGAL:
			this.pageSize = PageSize.LEGAL;
			break;
		case A3:
			this.pageSize = PageSize.A3;
			break;
		case A4:
			this.pageSize = PageSize.A4;
			break;
		case A5:
			this.pageSize = PageSize.A5;
			break;
		case B4:
			this.pageSize = PageSize.B4;
			break;
		case B5:
			this.pageSize = PageSize.B5;
			break;
		case EXECUTIVE:
			this.pageSize = PageSize.EXECUTIVE;
			break;
		case US4x6:
			this.pageSize = PageSize.US4x6;
			break;
		case US4x8:
			this.pageSize = PageSize.US4x8;
			break;
		case US5x7:
			this.pageSize = PageSize.US5x7;
			break;
		case COMM10:
			this.pageSize = PageSize.COMM10;
			break;
		default:
			Assert.fail();
		}
	}
	
	@Override
	public void destroy() {
		try {
			if (file.exists()) file.delete();
			String path = file.getAbsolutePath();
			Log.e(TAG, "path = "+path);
	 		doc.saveToFile(path);
	 		doc.destructAll();
		} catch (IOException e) {
			Log.e(TAG, "Error saving PDF file: "+e.toString());
		}	
	} 
	 
	public float scaledX(float x, float y) {
		if (rotate)
			return y * scale + offset_x;
		else
			return x * scale + offset_x;
	}
	
	public float scaledY(float x, float y) {
		if (rotate)
			return x * scale + offset_y;
		else
			return (1-y) * scale + offset_y;
	}
	
	@Override
	public void moveTo(float x, float y) {
		current_x = scaledX(x,y);
		current_y = scaledY(x,y);
		pdf.moveTo(current_x, current_y);
	}

	@Override
	public void lineTo(float x, float y) {
		current_x = scaledX(x,y);
		current_y = scaledY(x,y);
		pdf.lineTo(current_x, current_y);
	}

	@Override
	public void circle(float x, float y, float r) {
		current_x = scaledX(x,y);
		current_y = scaledY(x,y);
		pdf.circle(current_x, current_y, r * scale);
	}

	@Override
	public void ellipse(float x, float y, float xr, float yr) {
		current_x = scaledX(x,y);
		current_y = scaledY(x,y);
		pdf.ellipse(current_x, current_y, xr * scale, yr * scale);
	}

	// quadratic Bezier to (x2,y2) with control point (x1,y1)
	// Note: PDF only has cubic Bezier, but for special values of the two control points
	// the cubic degenerates to a quadratic Bezier. See http://fontforge.org/bezier.html
	public void quadTo(float x1, float y1, float x2, float y2)	{
		// the points of the quadratic Bezier
		float qx0 = current_x;
		float qy0 = current_y;
		float qx1 = scaledX(x1, y1);
		float qy1 = scaledY(x1, y1);
		float qx2 = scaledX(x2, y2);
		float qy2 = scaledY(x2, y2);
		// the two control points of the cubic Bezier
		float cx1 = 1f/3f * qx0 + 2f/3f * qx1;
		float cy1 = 1f/3f * qy0 + 2f/3f * qy1;
		float cx2 = 2f/3f * qx1 + 1f/3f * qx2;
		float cy2 = 2f/3f * qy1 + 1f/3f * qy2;
		pdf.curveTo(cx1, cy1, cx2, cy2, qx2, qy2);
		current_x = qx2;
		current_y = qy2;
	}
	
	// cubic Bezier to (x3,y3) with control points (x1,y1) and (x2,y2)
	public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)	{
		current_x = scaledX(x3,y3);
		current_y = scaledY(x3,y3);
		pdf.curveTo(scaledX(x1,y1), scaledY(x1,y1),
					scaledX(x2,y2), scaledY(x2,y2),
					current_x, current_y);
	}

	
	@Override
	public void stroke() {
		pdf.stroke();
		currentLineStyle = null;
	}

	@Override
	public void fill() {
		pdf.fill();
		currentLineStyle = null;
	}

	@Override
	public void fillStroke() {
		pdf.fillStroke();
		currentLineStyle = null;
	}

	@Override
	public void setLineWidth(float width) {
		float w = Math.max(0f, scale * width);
        pdf.setLineWidth(w);
	}

	@Override
	public void setLineColor(float red, float green, float blue) {
        pdf.setRGBStroke(red, green, blue);
	}

	@Override
	public void setLineCap(Cap cap) {
		switch (cap) {
		case BUTT_END:
			pdf.setLineCap(LineCap.BUTT_END);
			break;
		case PROJECTING_SQUARE_END:
			pdf.setLineCap(LineCap.PROJECTING_SQUARE_END);
			break;
		case ROUND_END:
			pdf.setLineCap(LineCap.ROUND_END);
			break;
		default:
			Assert.fail();
		}
	}

	@Override
	public void setLineJoin(Join join) {
		switch (join) {
		case BEVEL_JOIN:
			pdf.setLineJoin(LineJoin.BEVEL_JOIN);
			break;
		case MITER_JOIN:
			pdf.setLineJoin(LineJoin.MITER_JOIN);
			break;
		case ROUND_JOIN:
			pdf.setLineJoin(LineJoin.ROUND_JOIN);
			break;
		default:
			Assert.fail();
		}
	}
	
	public void setFillColor(float colorRed, float colorGreen, float colorBlue) {
		pdf.setRGBFill(colorRed, colorGreen, colorBlue);
	}
	
	public void addPageNumber() {
        int margin = 20;
        pdf.setFontAndSize(pageNumberFont, pageNumberSize);
        pdf.beginText();
        pdf.textOut(width-pageNumberSize-margin, height-pageNumberSize-margin,
        			""+pageNumber);
        pdf.endText();
        pageNumber++;
	}
		

	@Override
	public void imageJpeg(File jpgFile, float left, float right, float top, float bottom) {
		if (jpgFile == null)
			return;
		Image image = doc.getImage(jpgFile.getAbsolutePath());
		float x0 = scaledX(left,top);
		float y0 = scaledY(left,top);
		float x1 = scaledX(right,bottom);
		float y1 = scaledY(right,bottom);
		float width, height;
		if (x0 < x1) {
			width = x1 - x0;
		} else {
			width = x0 - x1;
			x0 = x1;
		}
		if (y0 < y1) {
			height = y1 - y0;
		} else {
			height = y0 - y1;
			y0 = y1;
		}
		pdf.image(image, x0, y0, width, height);
	}


	public void addPage(name.vbraun.view.write.Page page) {
		boolean page_is_portrait = (page.getAspectRatio() < 1);
		pdf = doc.addPage();
		Assert.assertNotNull(pageSize);
		if (page_is_portrait)	{
			pdf.setSize(pageSize, PageDirection.PORTRAIT);
		} else {
			pdf.setSize(pageSize, PageDirection.LANDSCAPE);
		}
		width = pdf.getWidth();
		height = pdf.getHeight();
		float page_aspect = page.getAspectRatio();
		float pdf_aspect = width / height;
		scale = Math.min(height, width/page_aspect);
		if (page_aspect < pdf_aspect) {
			float used_width = height * page_aspect;
			offset_y = 0;
			offset_x = (width - used_width)/2;
		} else {
			float used_height = width / page_aspect;
			offset_y = (height - used_height)/2;
			offset_x = 0;
		}
		page.render(this);
		if(page.getPaperType() == Type.MINUTES)
		{
			page.getAspectRatio();
			addTxtOnMinutesField();
		}
		if(page.getPaperType() == Type.DIARY)
		{
			page.getAspectRatio();
			addTxtOnDiaryField();
		}
		addPageNumber();
	}

	public void addTxtOnMinutesField() {
		 
        int margin = 20;
        float fontsize = 1.12f;
        float x, y, line, offsetx, offsety;
        float scale = 29.08f;
    	offsety = height - 39.0f;
		int nIndex = 1;
		boolean bEink6Inch = false;
		if (Hardware.isEinkUsingLargerUI()) {
	        scale = (float) (scale * 1.68);
	        fontsize = 1.54f;
	        offsety = height - 65.0f;
	        bEink6Inch = true;
	        nIndex = 0;
		}	        
        offsetx  = width /2 - margin/2;
        pdf.setFontAndSize(pageNumberFont, pageNumberSize*fontsize);
        pdf.beginText();
        x = margin;
        line = nIndex + 0;
    	y = offsety - line*scale;
    	pdf.textOut(x, y,"Page");
        line = nIndex + 1;
    	y = offsety - line*scale;
       	pdf.textOut(x, y,"Date");
       	pdf.textOut(offsetx, y,"Time");
        line = nIndex + 2;
    	y = offsety - line*scale;
       	pdf.textOut(x, y,"Agenda");
        line = nIndex + 4;
    	y = offsety - line*scale;
       	pdf.textOut(x, y,"Attendees");
        line = nIndex + 7;
    	y = offsety - line*scale;
       	pdf.textOut(x, y,"Note");
        pdf.endText();
	}

	public void addTxtOnDiaryField() {
        int margin = 14;
        float fontsize = 1.5f;
        float x, y, line, offsetx, offsety;
        float scale = 29.08f;
    	offsety = height - 34.0f;

		if (Hardware.isEinkUsingLargerUI()) {
	        scale = (float) (scale * 1.68);
	        fontsize = 1.82f;
	        offsety = height - 58.0f;
		}	        
		pdf.setFontAndSize(pageNumberFont, pageNumberSize*fontsize);
        pdf.beginText();
        x = margin;
        line = 1;
    	y = offsety - line*scale;
    	pdf.textOut(x, y,"Date     /");
    	pdf.endText();
	}
	
	public void addTestPage(name.vbraun.view.write.Page page) {
		Log.e(TAG, "Writing test page");
		setPaper(new PaperType(PaperType.PageSize.A4));
		addPage(page);
		try {
			doc.saveToFile("/mnt/sdcard/test.pdf");
		} catch (IOException e) {
			Log.e(TAG,e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void imageBackground(File jpgFile, float left, float right, float top, float bottom) {
		
		if (jpgFile == null)
			return;
		
		Image image = doc.getImage(jpgFile.getAbsolutePath());

		pdf.image(image, left, right, top, bottom);
		
		if (jpgFile.exists())
			jpgFile.delete();		
	}

	@Override
	public Page getPdf() {
		// TODO Auto-generated method stub
		return pdf;
	}

}
