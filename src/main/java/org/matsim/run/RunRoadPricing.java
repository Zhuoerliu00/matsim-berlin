package org.matsim.run;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;

public class RunRoadPricing {
	public static void main(String[] args) {
		// 加载配置文件并添加roadpricing模块
		Config config = ConfigUtils.loadConfig("D:\\2024SS\\Matsim\\matsim-berlin\\input\\v6.1\\berlin-v6.1-roadpricing.config.xml", new SimWrapperConfigGroup(), new RoadPricingConfigGroup());

		// 创建场景
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// 创建控制器
		Controler controler = new Controler(scenario);

		// 加载roadpricing方案
		//RoadPricingScheme roadPricingScheme = RoadPricingUtils.loadRoadPricingScheme(config, scenario.getNetwork());
		//RoadPricingScheme roadPricingScheme = RoadPricingUtils.loadRoadPricingSchemeAccordingToRoadPricingConfig(scenario);
		//controler.addOverridingModule(new RoadPricingModule(roadPricingScheme));
		controler.addOverridingModule(new SimWrapperModule());
		// 运行仿真
		controler.run();
	}
}

