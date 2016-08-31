package com.datatheorem.android.trustkit;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.datatheorem.android.trustkit.config.ConfigException;
import com.datatheorem.android.trustkit.config.PinnedDomainConfig;
import com.datatheorem.android.trustkit.config.TrustKitConfig;
import com.datatheorem.android.trustkit.report.BackgroundReporter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class TrustKit {

    private Context appContext;
    private TrustKitConfig trustKitConfig;
    private static TrustKit trustKitInstance;


    private TrustKit(Context context, TrustKitConfig trustKitConfig) {
        this.appContext = context;
        this.trustKitConfig = trustKitConfig;
    }

    public static TrustKit getInstance() {
        return trustKitInstance;
    }

    public static String init(Context context) {
        return generateTrustKitConfigFromNetworkSecurityConfig(context).toString();
    }

    public static void init(Context appContext, TrustKitConfig trustKitConfig) {
        if (trustKitInstance == null) {
            trustKitInstance = new TrustKit(appContext, trustKitConfig);
        }
    }

    private static TrustKitConfig generateTrustKitConfigFromNetworkSecurityConfig(Context context) {
        final int networkSecurityConfigId =
                context.getResources()
                        .getIdentifier("network_security_config", "xml", context.getPackageName());

        XmlResourceParser parser =
                context.getResources().getXml(networkSecurityConfigId);

        TrustKitConfig trustKitConfig = new TrustKitConfig();
        String domainName = null;
        PinnedDomainConfig.Builder pinnedDomainConfigBuilder = new PinnedDomainConfig.Builder();
        ArrayList<String> knownPins = null;
        boolean isADomain = false;
        boolean isAPin = false;
        boolean isAReportUri = false;
        ArrayList<String> reportUris = null;
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("domain".equals(parser.getName())){
                        isADomain = true;
                        pinnedDomainConfigBuilder
                                .includeSubdomains(parser.getAttributeBooleanValue(0, false));
                    } else if ("pin".equals(parser.getName())) {
                        isAPin = true;
                        isADomain = false;
                        if (knownPins == null) {
                            knownPins = new ArrayList<>();
                        }
                    } else if ("report-uri".equals(parser.getName())) {
                        isAReportUri = true;
                        isAPin = false;
                        isADomain = false;
                        if (reportUris == null) {
                            reportUris = new ArrayList<>();
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("domain".equals(parser.getName())) {
                        isADomain = false;
                    }

                    if ("pin".equals(parser.getName())) {
                        isAPin = false;
                    }


                    if ("report-uri".equals(parser.getName())){
                        isAReportUri = false;
                    }

                    if ("domain-config".equals(parser.getName())){
                        pinnedDomainConfigBuilder
                                .reportURIs(reportUris.toArray(new String[reportUris.size()]))
                                .publicKeyHashes(knownPins.toArray(new String[knownPins.size()]));
                        trustKitConfig.put(domainName, pinnedDomainConfigBuilder.build());
                    }


                } else if (eventType == XmlPullParser.TEXT) {
                    if (isADomain){
                        domainName = parser.getText();
                    }

                    if (isAPin) {
                        knownPins.add(parser.getText());
                    }

                    if (isAReportUri) {
                        reportUris.add(parser.getText());
                    }

                }

                eventType = parser.next();
            }

            if (trustKitConfig.size() < 0) {
                throw new ConfigException("something wrong with your configuration");
            }

            return trustKitConfig;

        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Context getAppContext() {
        return appContext;
    }

}
