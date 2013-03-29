/*
 * Copyright (C) 2012 Santiago Valdarrama
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.svpino.longhorn.layouts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.svpino.longhorn.artifacts.Extensions;

public class BorderRelativeLayout extends RelativeLayout {

	private ShapeDrawable shapeDrawable;
	private int border;
	private Paint paint;

	public BorderRelativeLayout(Context context) {
		super(context);
	}

	public BorderRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.border = Extensions.dpToPixels(getResources(), 3);

		this.shapeDrawable = new ShapeDrawable(new RectShape());

		this.paint = this.shapeDrawable.getPaint();
		this.paint.setStrokeWidth(Extensions.dpToPixels(getResources(), 1));
		this.paint.setColor(0xf2f2f2ff);
		this.paint.setStyle(Style.STROKE);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		updateDrawableBounds();

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		this.shapeDrawable.draw(canvas);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		this.shapeDrawable.setBounds(this.border, this.border, getWidth() - this.border, getHeight() - this.border);
	}

	private void updateDrawableBounds() {
		this.shapeDrawable.setBounds(this.border, this.border, getWidth() - this.border, getHeight() - this.border);
	}

}
