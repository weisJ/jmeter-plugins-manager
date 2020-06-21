package org.jmeterplugins.repository;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import com.github.weisj.darklaf.icons.IconLoader;
import com.github.weisj.darklaf.icons.ImageSource;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;
import org.apache.jmeter.gui.util.JMeterToolBar;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.logging.LoggingHooker;
import org.jmeterplugins.repository.util.ComponentFinder;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class PluginManagerMenuItem extends JMenuItem implements ActionListener {
    private static final long serialVersionUID = -8708638472918746046L;
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static PluginManagerDialog dialog;
    private final PluginManager mgr;

    public PluginManagerMenuItem() {
        super("Plugins Manager");
        addActionListener(this);

        mgr = new PluginManager(); // don't delay startup for longer that 1 second
        LoggingHooker hooker = new LoggingHooker(mgr);
        hooker.hook();
        final JButton toolbarButton = getToolbarButton();
        addToolbarIcon(toolbarButton);
        setIcon(getPluginsIcon(false));

        new Thread("repo-downloader-thread") {
            @Override
            public void run() {
                try {
                    mgr.load();
                } catch (Throwable e) {
                    log.warn("Failed to load plugin updates info", e);
                }

                if (mgr.hasAnyUpdates()) {
                    setText("Plugins Manager (has upgrades)");
                    log.info("Plugins Manager has upgrades: " + Arrays.toString(mgr.getUpgradablePlugins().toArray()));
                }

                boolean hasAnyUpdates = mgr.hasAnyUpdates();
                setIcon(getPluginsIcon(hasAnyUpdates));
                toolbarButton.setIcon(getIcon22Px(hasAnyUpdates));
                toolbarButton.setToolTipText(hasAnyUpdates ?
                        "Plugins Manager (has upgrades)" :
                        "Plugins Manager"
                );
            }
        }.start();
    }

    private void addToolbarIcon(final Component toolbarButton) {
        GuiPackage instance = GuiPackage.getInstance();
        if (instance != null) {
            final MainFrame mf = instance.getMainFrame();
            final ComponentFinder<JMeterToolBar> finder = new ComponentFinder<>(JMeterToolBar.class);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JMeterToolBar toolbar = null;
                    while (toolbar == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.debug("Did not add btn to toolbar", e);
                        }
                        log.debug("Searching for toolbar");
                        toolbar = finder.findComponentIn(mf);
                    }

                    int pos = toolbar.getComponents().length - 1;
                    toolbarButton.setSize(toolbar.getComponent(pos).getSize());
                    toolbar.add(toolbarButton, pos + 1);
                }
            });
        }
    }

    private JButton getToolbarButton() {
        boolean hasAnyUpdates = mgr.hasAnyUpdates();
        JButton button = new JButton(getIcon22Px(hasAnyUpdates));
        button.setToolTipText(hasAnyUpdates ?
                "Plugins Manager (has upgrades)" :
                "Plugins Manager"
        );
        button.addActionListener(this);
        return button;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new PluginManagerDialog(mgr);
        }

        dialog.pack();
        dialog.setVisible(true);
    }

    public static Icon getIcon22Px(boolean hasUpdates) {
        return getPluginsIcon(hasUpdates, 22);
    }

    public static Icon getPluginsIcon(boolean hasUpdates) {
        return getPluginsIcon(hasUpdates, 16);
    }

    public static Image getPluginImage(boolean hasUpdates) {
        Icon icon = getPluginsIcon(hasUpdates, 64);
        if (icon instanceof ImageSource) {
            return ((ImageSource) icon).createImage(64, 64);
        } else {
            BufferedImage img = createCompatibleTransparentImage(64, 64);
            Graphics g = img.getGraphics();
            icon.paintIcon(null, g, 0,0);
            g.dispose();
            return img;
        }
    }

    private static Icon getPluginsIcon(boolean hasUpdates, int size) {
        if (hasUpdates) {
            return IconLoader.get().getIcon("/org/jmeterplugins/logoUpdate.svg", size, size);
        } else {
            return IconLoader.get().getIcon("/org/jmeterplugins/logo.svg", size, size);
        }
    }

    private static BufferedImage createCompatibleTransparentImage(final int width, final int height) {
        return isHeadless() ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                            : getLocalGraphicsConfiguration().createCompatibleImage(width, height,
                                                                                    Transparency.BITMASK);
    }

    private static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    private static GraphicsConfiguration getLocalGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }
}
