package org.matsim.policies.gartenfeld;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.contrib.bicycle.BicycleConfigGroup;

public class RunSimWrapperPostprocessing {
	public static void main(String[] args) {
		String configFile = "/net/work/zliu/output/gartenfeld-v6.4.full-base-10pct-500-analysis/gartenfeld-v6.4.full-base-10pct.output_config.xml";

		// 加载 config，包括 bicycle 模块
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());

		// 注意这里是 "controller"，不是 "controler"
		ConfigGroup controller = config.getModules().get("controller");
		if (controller == null) {
			controller = new ConfigGroup("controller") {};
			config.addModule(controller);
		}

		// 强制设置 overwriteFiles=true，避免output目录存在时报错
		controller.addParam("overwriteFiles", "overwriteExistingFiles");

		// 初始化 Controler
		Controler controler = new Controler(config);

		// 加上SimWrapper模块（分析模块）
		controler.addOverridingModule(new SimWrapperModule());

		// 可以考虑是否需要其他分析模块，比如 CountsModule，但可以先不用
		// controler.addOverridingModule(new CountsModule());
		// controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());

		controler.run();
	}
}
