rm(list=ls())

require(effsize) 
source("C://code-R/JIT-locater-R/base.R")

root <- "C://data/JIT-defectlocater/R1/"

projects <- c("deeplearning4j", "jmeter", "h2o","libgdx", "jetty", "robolectric", "storm", "jitsi", "jenkins",
			"graylog2-server", "flink", "druid", "closure-compiler", "activemq")


remain_features <- c("commit_hash", "author_date_unix_timestamp", "author_date", "fix", 
	"ns", "nd", "nf", "entrophy", "la", "ld", "lt", "ndev", "age", "nuc", "exp", "rexp", "sexp", "contains_bug")

model_features <- c("fix", "ns", "nd", "nf", "entrophy", "la", "ld", "lt", 
	"ndev", "age", "nuc", "exp", "rexp", "sexp", "contains_bug")

log_features <- c("ns", "nd", "nf", "entrophy", "exp", "rexp", "sexp", "ndev", "age", "nuc", "la", "ld", "lt")

label <- "contains_bug"
positive_label <- "TRUE"
filter_features <- c(label)

for (p in projects){

	result_frame <- NULL
	fn <- paste(c(root, p, "/", p,"cbs_r1.csv"), collapse="")
	data <- read.csv(fn)
	
	temp_label <- tolower(data[label][,1])
	temp_label <- as.vector(temp_label)
	temp_label[which(temp_label=="false")] <- "FALSE"
	temp_label[which(temp_label=="true")] <- "TRUE"	
	temp_label_factor <- factor(temp_label, order=TRUE, levels=c("FALSE", "TRUE"))
	data[label] <- temp_label_factor
    data <- data[order(data$author_date_unix_timestamp),] # order by time, increasing order

	var_names <- names(data)
	var_names1 <- var_names[var_names %in% model_features]
	var_names1 <- var_names1[!var_names1 %in% filter_features]
	var_names_str <- paste(var_names1, collapse="+")
	print(var_names_str)
	#result_frame <- NULL

	form <- as.formula(paste(label, var_names_str, sep=" ~ "))
	var_names2 <- append(var_names1, label)
    var_names2 <- append("commit_hash", var_names2)
	temp_data <- data[var_names2]

	data_log <- get_log_data(temp_data, log_features)

	data_log$real_la <- data$la
	data_log$real_ld <- data$ld
	

	precision_scores <- c()
	recall_scores <- c()
	F1_scores <- c()


	# factorise labels
	buggy_labels <- factor(data_log[label][,1], order=TRUE, levels=c("FALSE", "TRUE"))
	data_log[label][,1] <- buggy_labels

    train_data <- NULL


	#fixed ratio validation
	test_ratio <- 0.4
	size <- dim(data_log)[1]
	test_size <- floor(test_ratio*size+0.5)
	train_size <- size - test_size

	train_start <- 1
	train_end <- train_size
	test_start <- train_end + 1
	test_end <- size

	train_data <- data_log[train_start:train_end,]
	test_data <- data_log[test_start:test_end,]

	train_data <- undersampling(train_data, label)


	fit <- glm(form, train_data, family=binomial(link = "logit"))
	prediction <- predict(fit, test_data, type="response")
	test_data$prob <- prediction
	test_data$pre_label <- 0

	test_data$pre_label[which(test_data$prob>0.5)] <- 1


	# random guess 
	rg <- runif(dim(test_data)[1], min = 0, max = 1)
	rg[which(rg>0.5)] <- 1
	rg[which(rg<0.5)] <- 0
	test_data$rg <- rg

	fn_output <- paste(c(root, p, "/", p,"cbs_result.csv"), collapse="")
	write.csv(test_data, fn_output, row.names=FALSE)
}	
