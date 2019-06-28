package ntx.note;

import name.vbraun.view.write.Paper;

public class PageBackgroundChangeEvent {
	private String backgroundType = Paper.EMPTY;

	public void setBackgroundType(String type) {
		this.backgroundType = type;
	}

	public String getBackgroundType() {
		return this.backgroundType;
	}

}
