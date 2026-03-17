package PDF.Services.V2Services;

import Form.FieldType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class ParsePDFFieldsService {

  private final InputStream pdfStream;
  private JSONArray extractedFields;

  public ParsePDFFieldsService(InputStream pdfStream) {
    this.pdfStream = pdfStream;
  }

  public JSONArray getExtractedFields() {
    return extractedFields;
  }

  public boolean execute() {
    try {
      byte[] bytes = pdfStream.readAllBytes();
      PDDocument doc = Loader.loadPDF(bytes);
      doc.setAllSecurityToBeRemoved(true);
      PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
      if (acroForm == null) {
        return false;
      }
      List<PDPage> pages = new ArrayList<>();
      for (PDPage p : doc.getPages()) {
        pages.add(p);
      }
      List<JSONObject> fields = new ArrayList<>();
      extractFields(acroForm.getFields(), pages, fields);
      this.extractedFields = new JSONArray(fields);
      doc.close();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private void extractFields(
      List<PDField> pdFields, List<PDPage> pages, List<JSONObject> result) {
    for (PDField field : pdFields) {
      if (field instanceof PDNonTerminalField) {
        extractFields(((PDNonTerminalField) field).getChildren(), pages, result);
      } else if (field instanceof PDPushButton || field instanceof PDSignatureField) {
        continue;
      } else {
        JSONObject obj = new JSONObject();
        obj.put("fieldName", field.getFullyQualifiedName());
        obj.put("fieldType", detectFieldType(field));
        obj.put("readOnly", field.isReadOnly());
        Set<String> optionSet = new LinkedHashSet<>();
        if (isRadioField(field) && field instanceof PDButton) {
          optionSet.addAll(((PDButton) field).getOnValues());
        } else if (field instanceof PDChoice) {
          List<String> options = ((PDChoice) field).getOptions();
          optionSet.addAll(options);
        } else if (field instanceof PDCheckBox) {
          optionSet.add(((PDCheckBox) field).getOnValue());
        }

        List<PDAnnotationWidget> widgets = field.getWidgets();
        if (widgets != null && !widgets.isEmpty()) {
          JSONArray widgetRects = new JSONArray();
          JSONObject firstWidget = null;
          for (PDAnnotationWidget widget : widgets) {
            PDRectangle rect = widget.getRectangle();
            if (rect == null) {
              continue;
            }
            int pageIndex = getPageIndex(widget.getPage(), pages);
            JSONObject widgetObj = new JSONObject();
            widgetObj.put("page", pageIndex);
            widgetObj.put(
                "rect",
                new JSONArray(
                    List.of(
                        rect.getLowerLeftX(),
                        rect.getLowerLeftY(),
                        rect.getWidth(),
                        rect.getHeight())));
            String widgetOption = getWidgetOnState(widget);
            if (widgetOption != null && !widgetOption.isEmpty()) {
              widgetObj.put("option", widgetOption);
              optionSet.add(widgetOption);
            }
            widgetRects.put(widgetObj);
            if (firstWidget == null) {
              firstWidget = widgetObj;
            }
          }

          if (firstWidget != null) {
            obj.put("page", firstWidget.getInt("page"));
            obj.put("rect", firstWidget.getJSONArray("rect"));
          } else {
            obj.put("page", 0);
            obj.put("rect", new JSONArray(List.of(0, 0, 0, 0)));
          }
          obj.put("widgetRects", widgetRects);
        } else {
          obj.put("page", 0);
          obj.put("rect", new JSONArray(List.of(0, 0, 0, 0)));
          obj.put("widgetRects", new JSONArray());
        }
        if (!optionSet.isEmpty()) {
          obj.put("options", new JSONArray(optionSet));
        }

        result.add(obj);
      }
    }
  }

  private String detectFieldType(PDField field) {
    if (isRadioField(field)) return FieldType.RADIO_BUTTON.toString();
    if (field instanceof PDCheckBox) return FieldType.CHECKBOX.toString();
    if (field instanceof PDComboBox) return FieldType.COMBOBOX.toString();
    if (field instanceof PDListBox) return FieldType.LISTBOX.toString();
    if (field instanceof PDTextField) {
      PDTextField tf = (PDTextField) field;
      if (tf.isReadOnly()) return FieldType.READ_ONLY_FIELD.toString();
      if (tf.isMultiline()) return FieldType.MULTILINE_TEXT_FIELD.toString();
      return FieldType.TEXT_FIELD.toString();
    }
    return FieldType.TEXT_FIELD.toString();
  }

  private boolean isRadioField(PDField field) {
    return field instanceof PDButton && ((PDButton) field).isRadioButton();
  }

  private int getPageIndex(PDPage widgetPage, List<PDPage> pages) {
    if (widgetPage == null) {
      return 0;
    }
    for (int i = 0; i < pages.size(); i++) {
      if (pages.get(i).equals(widgetPage)) {
        return i;
      }
    }
    return 0;
  }

  private String getWidgetOnState(PDAnnotationWidget widget) {
    try {
      COSDictionary widgetDict = widget.getCOSObject();
      COSBase apBase = widgetDict.getDictionaryObject(COSName.AP);
      if (!(apBase instanceof COSDictionary)) {
        return null;
      }
      COSBase normalBase = ((COSDictionary) apBase).getDictionaryObject(COSName.N);
      if (!(normalBase instanceof COSDictionary)) {
        return null;
      }
      COSDictionary normalDict = (COSDictionary) normalBase;
      for (COSName key : normalDict.keySet()) {
        String name = key.getName();
        if (!"Off".equalsIgnoreCase(name)) {
          return name;
        }
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }
}
