/*
 * Copyright (c) 2017. Nuvolect LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Contact legal@nuvolect.com for a less restrictive commercial license if you would like to use the
 * software without the GPLv3 restrictions.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not,
 * see <http://www.gnu.org/licenses/>.
 *
 */

package com.nuvolect.securesuite.graphics;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.Transformation;
import android.widget.ImageView;

public class RoundedTransformationBuilder {

    //private final Resources mResources;
    private final DisplayMetrics mDisplayMetrics;

    private float mCornerRadius = 0;
    private boolean mOval = false;
    private float mBorderWidth = 0;
    private ColorStateList mBorderColor =
        ColorStateList.valueOf(RoundedDrawable.DEFAULT_BORDER_COLOR);
    private ImageView.ScaleType mScaleType = ImageView.ScaleType.FIT_CENTER;

    public RoundedTransformationBuilder() {
      mDisplayMetrics = Resources.getSystem().getDisplayMetrics();
    }

    public RoundedTransformationBuilder scaleType(ImageView.ScaleType scaleType) {
      mScaleType = scaleType;
      return this;
    }

    /**
     * set corner radius in px
     */
    public RoundedTransformationBuilder cornerRadius(float radiusPx) {
      mCornerRadius = radiusPx;
      return this;
    }

    /**
     * set corner radius in dip
     */
    public RoundedTransformationBuilder cornerRadiusDp(float radiusDp) {
      mCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radiusDp, mDisplayMetrics);
      return this;
    }

    /**
     * set border width in px
     */
    public RoundedTransformationBuilder borderWidth(float widthPx) {
      mBorderWidth = widthPx;
      return this;
    }

    /**
     * set border width in dip
     */
    public RoundedTransformationBuilder borderWidthDp(float widthDp) {
      mBorderWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, mDisplayMetrics);
      return this;
    }


    /**
     * set border color
     */
    public RoundedTransformationBuilder borderColor(int color) {
      mBorderColor = ColorStateList.valueOf(color);
      return this;
    }

    public RoundedTransformationBuilder borderColor(ColorStateList colors) {
      mBorderColor = colors;
      return this;
    }

    public RoundedTransformationBuilder oval(boolean oval) {
      mOval = oval;
      return this;
    }

    public Transformation build() {
      return new Transformation() {
//        @Override public Bitmap transform(Bitmap source) {
        public Bitmap transform(Bitmap source) {
          Bitmap transformed = RoundedDrawable.fromBitmap(source)
              .setScaleType(mScaleType)
              .setCornerRadius(mCornerRadius)
              .setBorderWidth(mBorderWidth)
              .setBorderColor(mBorderColor)
              .setOval(mOval)
              .toBitmap();
          if (!source.equals(transformed)) {
            source.recycle();
          }
          return transformed;
        }

//        @Override public String key() {
        public String key() {
          return "r:" + mCornerRadius
              + "b:" + mBorderWidth
              + "c:" + mBorderColor
              + "o:" + mOval;
        }
      };
    }

}
