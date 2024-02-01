package com.abilsys.oa.quartz.util;

import com.cronutils.builder.CronBuilder;
import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronConstraintsFactory;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.value.SpecialChar;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Locale;

import static com.cronutils.model.CronType.QUARTZ;
import static com.cronutils.model.CronType.UNIX;
import static com.cronutils.model.field.expression.FieldExpressionFactory.*;

public class CronUtilTest {
    
    @Test
    public void defineOwnCronDefinition() {
        // define your own cron: arbitrary fields are allowed and last field can be optional
        com.cronutils.model.definition.CronDefinition instance = CronDefinitionBuilder.defineCron().withSeconds().and().withMinutes().and().withHours().and().withDayOfMonth()
                .supportsHash().supportsL().supportsW().and().withMonth().and().withDayOfWeek().withIntMapping(7, 0) // we
                // support
                // non-standard
                // non-zero-based
                // numbers!
                .supportsHash().supportsL().supportsW().and().withYear().optional().and().instance();
        System.out.println("instance = " + instance);
    }

    /**
     * This method will walk you through how to create cron definitions: by using pre-defined ones or building your own.
     */
    @Test
    public void howToWithCronDefinitions() {
        // Define your own cron: arbitrary fields are allowed and last field can be optional
        CronDefinition cronDefinition1 = CronDefinitionBuilder.defineCron().withSeconds().and().withMinutes().and()
                .withHours().and().withDayOfMonth().supportsL().supportsW().supportsLW().supportsQuestionMark().and()
                .withMonth().and().withDayOfWeek().withValidRange(1, 7).withMondayDoWValue(2).supportsHash().supportsL()
                .supportsQuestionMark().and().withYear().withValidRange(1970, 2099).optional().and()
                .withCronValidation(CronConstraintsFactory.ensureEitherDayOfWeekOrDayOfMonth()).instance();

        // or get a predefined instance
        CronDefinition cronDefinition2 = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);

        System.out.println(String.format("Are those definitions the same? %s", cronDefinition1.equals(cronDefinition2)));
        // now you can use it to build a parser or to programatically build cron expressions!
    }

    /**
     * This method will walk you through how to parse cron expressions.
     */
    @Test
    public void howToWithCronParser() {
        // once we created a cron definition, we can use it to build a parser
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser parser = new CronParser(cronDefinition);
        String expression = "0 23 * * * ? *";
        Cron quartzCron = parser.parse(expression);

        System.out.println(
                String.format("Was the parsing correct? Lets turn it to string again! original: '%s' parsed: '%s'",
                        expression, quartzCron.asString())
        );

        /*
         * We can even parse multi-crons! How about squashing multiple crons into a single line? Instead of writting
         * "0 0 9 * * ? *", "0 0 10 * * ? *", "0 30 11 * * ? *" and "0 0 12 * * ? *" we can wrap it into
         * "0 0|0|30|0 9|10|11|12 * * ? *"
         */
        String multicron = "0 0|0|30|0 9|10|11|12 * * ? *";
        Cron cron = parser.parse(multicron);
        System.out.println(String.format("Are those the same? %s", multicron.equals(cron.asString())));
    }

    /**
     * This method will walk you through how to programatically build cron expressions
     */
    @Test
    public void howToBuildAndMigrateCronExpressions() {
        // we can define crons programmatically
        Cron unixBuiltCronExpression =
                CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(UNIX)).withDoM(between(1, 3)).withMonth(always())
                        .withDoW(always()).withHour(always()).withMinute(always()).instance();
        String unixBuiltCronExpressionString = unixBuiltCronExpression.asString();

        Cron quartzBuiltCronExpression = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ))
                .withYear(always()).withDoM(between(1, 3)).withMonth(always()).withDoW(questionMark()).withHour(always())
                .withMinute(always()).withSecond(on(0)).instance();
        String quartzBuiltCronExpressionString = quartzBuiltCronExpression.asString();

        /*
         * Definitions seem coupled to a specific representation ... To avoid that, we can migrate anytime using a
         * CronMapper :) Let's see how we do that! Below we provide some scenarios, where this can be useful.
         */

        // What if we are migrating between cron formats?
        // Ex.: we had everything in Linux, and now we provide a Quartz based scheduling service with a nice REST API.
        // How can we perform the conversions, to remain equivalent while minimizing work?
        // cron-utils provides a CronMapper for that: we can also migrate from/to any other cron format
        String fromQuartzToUnixString = CronMapper.fromQuartzToUnix().map(quartzBuiltCronExpression).asString();
        String fromUnixToQuartzString = CronMapper.fromUnixToQuartz().map(unixBuiltCronExpression).asString();

        // Lets compare this expressions
        System.out.println(String.format("Original Quartz cron expression: '%s' Mapped from Unix: '%s'",
                quartzBuiltCronExpressionString, fromUnixToQuartzString)
        );
        System.out.println(String.format("Original Unix cron expression: '%s' Mapped from Quartz: '%s'",
                unixBuiltCronExpressionString, fromQuartzToUnixString)
        );

        // If we are not performing a migration, but just need to check if expressions are equivalent ...
        System.out.println(String.format("Are both expressions (Quartz: '%s' vs. Unix: '%s') equivalent? %s",
                quartzBuiltCronExpressionString, unixBuiltCronExpressionString,
                quartzBuiltCronExpression.equivalent(CronMapper.fromUnixToQuartz(), unixBuiltCronExpression))
        );
    }

    @Test
    public void howToGetExecutionTimes() {

//        String quartzCronExpression = "0 * * 1-3 * ? *";
        String quartzCronExpression = "* * 5 3 * ? *";
        CronParser quartzCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));

        // parse the QUARTZ cron expression.
        Cron parsedQuartzCronExpression = quartzCronParser.parse(quartzCronExpression);

        // Create ExecutionTime for a given cron expression.
        ZonedDateTime now = ZonedDateTime.now();
        ExecutionTime executionTime = ExecutionTime.forCron(parsedQuartzCronExpression);

        // Given a Cron instance, we can ask for next/previous execution
        System.out.println(String.format("Given the Quartz cron '%s' and reference date '%s', last execution was '%s'",
                parsedQuartzCronExpression.asString(), now, executionTime.lastExecution(now).get())
        );
        System.out.println(String.format("Given the Quartz cron '%s' and reference date '%s', next execution will be '%s'",
                parsedQuartzCronExpression.asString(), now, executionTime.nextExecution(now).get())
        );

        // or request time from last / to next execution
        System.out
                .println(String.format("Given the Quartz cron '%s' and reference date '%s', last execution was %s seconds ago",
                        parsedQuartzCronExpression.asString(), now, executionTime.timeFromLastExecution(now).get().getSeconds())
                );
        System.out.println(
                String.format("Given the Quartz cron '%s' and reference date '%s', next execution will be in %s seconds",
                        parsedQuartzCronExpression.asString(), now, executionTime.timeToNextExecution(now).get().getSeconds())
        );

    }

    @Test
    public void howToHumanReadableDescriptions() {
        // we first need to setup a parser
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser parser = new CronParser(cronDefinition);
        String expression = "* 23 * ? * 1-5 *";

        // and then just ask for a description
        CronDescriptor descriptor = CronDescriptor.instance(Locale.US);// we support multiple languages! Just pick one!
        String quartzBuiltCronExpressionDescription = descriptor.describe(parser.parse(expression));
        System.out.println(
                String.format("Quartz expression '%s' is described as '%s'", expression, quartzBuiltCronExpressionDescription)
        );
    }

    @Test
    public void makeCron() {
        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                .withYear(always())
                .withDoW(questionMark())
                .withMonth(always())
                .withDoM(always())
                .withHour(always())
                .withMinute(on(10))
                .withSecond(on(0))
                .instance();
// Obtain the string expression
        String cronAsString = cron.asString(); // 0 * * L-3 * ? *
        System.out.println("cronAsString = " + cronAsString);

        String[] s = cronAsString.split(" ");
        for (String string : s) {
            System.out.println("string = " + string);
        }

        cron.validate();
    }


    @Test
    public void findIncludeValue() {
    }
}