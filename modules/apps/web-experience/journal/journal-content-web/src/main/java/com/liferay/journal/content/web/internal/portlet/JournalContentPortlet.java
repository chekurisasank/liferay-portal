/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.journal.content.web.internal.portlet;

import com.liferay.journal.constants.JournalWebKeys;
import com.liferay.journal.content.web.constants.JournalContentPortletKeys;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalArticleDisplay;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.journal.util.JournalContent;
import com.liferay.journal.web.util.ExportArticleUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portlet.PortletRequestModel;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PrefsParamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.trash.kernel.service.TrashEntryService;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eudaldo Alonso
 */
@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.add-default-resource=true",
		"com.liferay.portlet.css-class-wrapper=portlet-journal-content",
		"com.liferay.portlet.display-category=category.cms",
		"com.liferay.portlet.display-category=category.highlighted",
		"com.liferay.portlet.header-portlet-css=/css/main.css",
		"com.liferay.portlet.icon=/icons/journal_content.png",
		"com.liferay.portlet.instanceable=true",
		"com.liferay.portlet.layout-cacheable=true",
		"com.liferay.portlet.preferences-owned-by-group=true",
		"com.liferay.portlet.private-request-attributes=false",
		"com.liferay.portlet.private-session-attributes=false",
		"com.liferay.portlet.render-weight=50",
		"com.liferay.portlet.scopeable=true",
		"com.liferay.portlet.use-default-template=true",
		"javax.portlet.display-name=Web Content Display",
		"javax.portlet.expiration-cache=0",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.name=" + JournalContentPortletKeys.JOURNAL_CONTENT,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=guest,power-user,user",
		"javax.portlet.supports.mime-type=text/html",
		"javax.portlet.supports.mime-type=application/vnd.wap.xhtml+xml"
	},
	service = Portlet.class
)
public class JournalContentPortlet extends MVCPortlet {

	@Override
	public void doView(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		PortletPreferences portletPreferences = renderRequest.getPreferences();

		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long articleGroupId = PrefsParamUtil.getLong(
			portletPreferences, renderRequest, "groupId",
			themeDisplay.getScopeGroupId());

		String articleId = PrefsParamUtil.getString(
			portletPreferences, renderRequest, "articleId");
		String ddmTemplateKey = PrefsParamUtil.getString(
			portletPreferences, renderRequest, "ddmTemplateKey");

		JournalArticle article = null;
		JournalArticleDisplay articleDisplay = null;

		if ((articleGroupId > 0) && Validator.isNotNull(articleId)) {
			String viewMode = ParamUtil.getString(renderRequest, "viewMode");
			String languageId = LanguageUtil.getLanguageId(renderRequest);
			int page = ParamUtil.getInteger(renderRequest, "page", 1);

			article = _journalArticleLocalService.fetchLatestArticle(
				articleGroupId, articleId, WorkflowConstants.STATUS_APPROVED);

			try {
				if (article == null) {
					article = _journalArticleLocalService.getLatestArticle(
						articleGroupId, articleId,
						WorkflowConstants.STATUS_ANY);
				}

				if (Validator.isNull(ddmTemplateKey)) {
					ddmTemplateKey = article.getDDMTemplateKey();
				}

				articleDisplay = _journalContent.getDisplay(
					article, ddmTemplateKey, viewMode, languageId, page,
					new PortletRequestModel(renderRequest, renderResponse),
					themeDisplay);
			}
			catch (Exception e) {
				renderRequest.removeAttribute(WebKeys.JOURNAL_ARTICLE);
			}
		}

		if (article != null) {
			renderRequest.setAttribute(WebKeys.JOURNAL_ARTICLE, article);
		}

		if (articleDisplay != null) {
			renderRequest.setAttribute(
				WebKeys.JOURNAL_ARTICLE_DISPLAY, articleDisplay);
		}
		else {
			renderRequest.removeAttribute(WebKeys.JOURNAL_ARTICLE_DISPLAY);
		}

		super.doView(renderRequest, renderResponse);
	}

	@Override
	public void render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		renderRequest.setAttribute(
			JournalWebKeys.JOURNAL_CONTENT, _journalContent);

		super.render(renderRequest, renderResponse);
	}

	public void restoreJournalArticle(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long classPK = ParamUtil.getLong(actionRequest, "classPK");

		_trashEntryService.restoreEntry(
			JournalArticle.class.getName(), classPK);
	}

	@Override
	public void serveResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws IOException, PortletException {

		String resourceID = GetterUtil.getString(
			resourceRequest.getResourceID());

		if (resourceID.equals("exportArticle")) {
			String targetExtension = ParamUtil.getString(
				resourceRequest, "targetExtension");

			targetExtension = StringUtil.toUpperCase(targetExtension);

			PortletPreferences portletPreferences =
				resourceRequest.getPreferences();

			String[] allowedExtensions = StringUtil.split(
				portletPreferences.getValue(
					"userToolAssetAddonEntryKeys", null));

			if (ArrayUtil.contains(
					allowedExtensions,
					"enable" + StringUtil.toUpperCase(targetExtension))) {

				_exportArticleUtil.sendFile(
					targetExtension, resourceRequest, resourceResponse);
			}
		}
		else {
			resourceRequest.setAttribute(
				JournalWebKeys.JOURNAL_CONTENT, _journalContent);

			super.serveResource(resourceRequest, resourceResponse);
		}
	}

	@Reference(unbind = "-")
	protected void setExportArticleUtil(ExportArticleUtil exportArticleUtil) {
		_exportArticleUtil = exportArticleUtil;
	}

	@Reference(unbind = "-")
	protected void setJournalContent(JournalContent journalContent) {
		_journalContent = journalContent;
	}

	@Reference(unbind = "-")
	protected void setJournalContentSearchLocal(
		JournalArticleLocalService journalArticleLocalService) {

		_journalArticleLocalService = journalArticleLocalService;
	}

	@Reference(unbind = "-")
	protected void setTrashEntryService(TrashEntryService trashEntryService) {
		_trashEntryService = trashEntryService;
	}

	protected void unsetExportArticleUtil(ExportArticleUtil exportArticleUtil) {
		_exportArticleUtil = exportArticleUtil;
	}

	protected void unsetJournalContent(JournalContent journalContent) {
		_journalContent = null;
	}

	protected void unsetJournalContentSearchLocal(
		JournalArticleLocalService journalArticleLocalService) {

		_journalArticleLocalService = null;
	}

	private ExportArticleUtil _exportArticleUtil;
	private JournalArticleLocalService _journalArticleLocalService;
	private JournalContent _journalContent;
	private TrashEntryService _trashEntryService;

}