//-------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.txt file in the project root for full license information.
//-------------------------------------------------------------------------------------------------------
package Windows.UI.Xaml.Shapes;

import android.content.Context;
import android.graphics.*;
import android.graphics.Color;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import Windows.UI.Xaml.Controls.*;

public class Path extends Shape {
  Geometry _data;
  Paint _strokePaint;
  Paint _fillPaint;
  RectF _bounds;

  public Path(Context context) {
    super(context);
    this.setWillNotDraw(false);

    _strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    _strokePaint.setStyle(Paint.Style.STROKE);

    _fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    _fillPaint.setStyle(Paint.Style.FILL);

    // Default property values
    _strokeStartLineCap = "Flat";
    _strokeEndLineCap = "Flat";
    _strokeThickness = 1.0;
  }

  // IHaveProperties.setProperty
  @Override
	public void setProperty(String propertyName, Object propertyValue) {
    if (propertyName.endsWith(".Data")) {
      if (propertyValue instanceof Geometry) {
        _data = (Geometry)propertyValue;
      }
      else {
        _data = GeometryConverter.parse((String)propertyValue);
      }
      // Redraw
      invalidate();
    }
    else {
      super.setProperty(propertyName, propertyValue);
    }
	}

  @Override
  protected void onDraw(android.graphics.Canvas canvas) {
    // TODO: Respect Shape stretch
    super.onDraw(canvas);

    if (_data == null) {
      return;
    }

    //TODO: Otherwise the top/left strokes get cut off
    int shift = 1;

    // Stroke
    int strokeColor = Color.TRANSPARENT;
    if (_stroke instanceof SolidColorBrush) {
      strokeColor = ((SolidColorBrush)_stroke).Color;
    }
    else if (_stroke != null) {
      throw new RuntimeException("Unhandled stroke type: " + _stroke.getClass().getSimpleName());
    }
    _strokePaint.setColor(strokeColor);
    _strokePaint.setStrokeWidth((float)getStrokeThickness());

    // Fill
    int fillColor = Color.TRANSPARENT;
    if (_fill instanceof SolidColorBrush) {
      fillColor = ((SolidColorBrush) _fill).Color;
    }
    else if (_fill != null) {
      throw new RuntimeException("Unhandled fill type: " + _fill.getClass().getSimpleName());
    }
    _fillPaint.setColor(fillColor);

    if (!(_data instanceof PathGeometry)) {
      throw new RuntimeException("Unsupported type of Geometry: " + _data.getClass().getSimpleName());
    }

    final android.graphics.Path path = new android.graphics.Path();
    //TODO: Different fill types
    path.setFillType(android.graphics.Path.FillType.EVEN_ODD);

    PathFigureCollection figures = ((PathGeometry)_data).figures;
    for (int i = 0; i < figures.size(); i++) {
      PathFigure figure = (PathFigure)figures.get(i);
      PointF previousPoint = figure.getStartPoint();
      PointF startPoint = previousPoint;
      path.moveTo(startPoint.x + shift, startPoint.y + shift);

      // Loop thru segments
      PathSegmentCollection segments = (PathSegmentCollection)figure.getSegments();
      for (int j = 0; j < segments.size(); j++) {
        PathSegment segment = (PathSegment)segments.get(j);

        if (segment instanceof LineSegment) {
          PointF currentPoint = ((LineSegment)segment).getPoint();
          path.lineTo(currentPoint.x + shift, currentPoint.y + shift);
          previousPoint = currentPoint;
        }
        else if (segment instanceof PolyLineSegment) {
          PointCollection points = ((PolyLineSegment)segment).getPoints();
          for (int k = 0; k < points.size(); k++) {
            PointF currentPoint = (PointF) points.get(k);
            path.lineTo(currentPoint.x + shift, currentPoint.y + shift);
            previousPoint = currentPoint;
          }
        }
        else if (segment instanceof ArcSegment) {
          throw new RuntimeException("NYI: ArcSegment");
        }
        else if (segment instanceof BezierSegment) {
          PointF controlPoint1 = ((BezierSegment)segment).getPoint1();
          PointF controlPoint2 = ((BezierSegment)segment).getPoint2();
          PointF currentPoint = ((BezierSegment)segment).getPoint3();
          path.cubicTo(controlPoint1.x + shift, controlPoint1.y + shift, controlPoint2.x + shift, controlPoint2.y + shift, currentPoint.x + shift, currentPoint.y + shift);
          previousPoint = currentPoint;
        }
        else if (segment instanceof PolyBezierSegment) {
          PointCollection points = ((PolyBezierSegment)segment).getPoints();
          for (int k = 0; k < points.size(); k++) {
            PointF controlPoint1 = (PointF) points.get(k);
            k++;
            PointF controlPoint2 = (PointF) points.get(k);
            k++;
            PointF currentPoint = (PointF) points.get(k);

            path.cubicTo(controlPoint1.x + shift, controlPoint1.y + shift, controlPoint2.x + shift, controlPoint2.y + shift, currentPoint.x + shift, currentPoint.y + shift);
            previousPoint = currentPoint;
          }
        }
        else if (segment instanceof QuadraticBezierSegment) {
          PointF controlPoint = ((QuadraticBezierSegment)segment).getPoint1();
          PointF currentPoint = ((QuadraticBezierSegment)segment).getPoint2();
          path.quadTo(controlPoint.x + shift, controlPoint.y + shift, currentPoint.x + shift, currentPoint.y + shift);
          previousPoint = currentPoint;
        }
        else if (segment instanceof PolyQuadraticBezierSegment) {
          PointCollection points = ((PolyQuadraticBezierSegment)segment).getPoints();
          for (int k = 0; k < points.size(); k++) {
            PointF controlPoint = (PointF) points.get(k);
            k++;
            PointF currentPoint = (PointF) points.get(k);

            path.quadTo(controlPoint.x + shift, controlPoint.y + shift, currentPoint.x + shift, currentPoint.y + shift);
            previousPoint = currentPoint;
          }
        }
      } // done looping through segments

      if (figure.getIsClosed()) {
          //TODO: path.lineTo(startPoint.x + shift, startPoint.y + shift);
          path.close();
      }
    } // done looping through figures

    /*TODO scaling
    if (false) {
        Matrix m = new Matrix();
        m.setScale(Viewbox.Scale,Viewbox.Scale);
        path.transform(m);
        _strokePaint.setStrokeWidth((float)(double)get_StrokeThickness() * Viewbox.Scale);
    }*/

    if (_fill instanceof LinearGradientBrush) {
      RectF bounds = new RectF();
      path.computeBounds(bounds, false);

      // TODO: Support more fully (such as StartPoint/EndPoint)
      LinearGradientBrush lgb = (LinearGradientBrush)_fill;
      int count = lgb.getGradientStops().size();
      int[] colors = new int[count];
      float[] offsets = new float[count];
      for (int i = 0; i < count; i++) {
        colors[i] = ((GradientStop)lgb.getGradientStops().get(i)).color;
        offsets[i] = (float)((GradientStop)lgb.getGradientStops().get(i)).offset;
      }

      // TODO: Vertical only
      _fillPaint.setShader(new LinearGradient(0f, 0f, 0f, (bounds.bottom - bounds.top), colors[0], colors[1], Shader.TileMode.MIRROR));
      _fillPaint.setColor(Color.WHITE); // TODO: Need to fill with a color for some reason
    }

    if (_stroke != null)
      canvas.drawPath(path, _strokePaint);
    if (_fill != null)
      canvas.drawPath(path, _fillPaint);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      _bounds = new RectF(0, 0, w, h);
  }
}
