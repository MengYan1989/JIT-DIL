


get_log_data<-function(temp_data, log_features){
	for (feature in log_features){
		temp_vec <- temp_data[,feature]
		temp_vec[is.na(temp_vec)] <- 0
		temp_vec <- as.vector(temp_vec)
		temp_vec_num <- as.numeric(temp_vec)
		temp_vec_num[is.na(temp_vec_num)] <- 0	
		temp_vec_num[temp_vec_num>0] <- log(temp_vec_num[temp_vec_num>0]+1)
		temp_data[feature] <- temp_vec_num
		#print(feature)
	}

	return(temp_data)
}


get_normalized_data_train <-function(temp_data, log_features){
	for (feature in log_features){
		temp_vec <- temp_data[,feature]
		temp_vec[is.na(temp_vec)] <- 0
		temp_vec <- as.vector(temp_vec)
		temp_vec_num <- as.numeric(temp_vec)
		temp_vec_num[is.na(temp_vec_num)] <- 0
		max <- max(temp_vec_num)
		min <- min(temp_vec_num)
		if (max > min)	
		  temp_vec_num[temp_vec_num>0] <- (temp_vec_num[temp_vec_num>0]-min)/(max-min)

		temp_data[feature] <- temp_vec_num
		#print(feature)
	}

	return(temp_data)
}

get_normalized_data_test <-function(train_data, test_data, log_features){
	for (feature in log_features){
		temp_vec <- train_data[,feature]
		temp_vec[is.na(temp_vec)] <- 0
		temp_vec <- as.vector(temp_vec)
		temp_vec_num <- as.numeric(temp_vec)
		temp_vec_num[is.na(temp_vec_num)] <- 0
		max <- max(temp_vec_num)
		min <- min(temp_vec_num)

		temp_vec_test <- test_data[,feature]
		temp_vec_test[is.na(temp_vec_test)] <- 0
		temp_vec_test <- as.vector(temp_vec_test)
		temp_vec_num_test <- as.numeric(temp_vec_test)

		if (max > min)	
		  temp_vec_num_test[temp_vec_num_test>0] <- (temp_vec_num_test[temp_vec_num_test>0]-min)/(max-min)

		test_data[feature] <- temp_vec_num_test
		#print(feature)
	}

	return(test_data)
}


store_result_to_frame<-function(result_frame, scores_vector){
	temp_frame <- data.frame(scores_vector)
	if (is.null(result_frame)){
		result_frame <- temp_frame
		}
	else {
		result_frame <- cbind(result_frame, temp_frame)
	}
	return(result_frame)
}

undersampling <- function(data, class){
	buggy_data <- data[which(data[class][,1] == positive_label),]
	clean_data <- data[-(which(data[class][,1] == positive_label)),]

	buggy_size <- dim(buggy_data)[1]
	clean_size <- dim(clean_data)[1]
	if (clean_size > buggy_size){
		sampled_index <- sample(nrow(clean_data), nrow(buggy_data))
		sampled_data <- clean_data[sampled_index,]
		ret_data <- rbind(buggy_data, sampled_data)
	}
    else{
    	sampled_index <- sample(nrow(buggy_data), nrow(clean_data))
		sampled_data <- buggy_data[sampled_index,]
		ret_data <- rbind(sampled_data, clean_data)
    }

	return(ret_data)
}

label_to_large_case <- function (data, label) {

    temp_label <- data[label][,1]
	temp_label <- as.vector(temp_label)
	temp_label[which(temp_label=="False")] <- "FALSE"
	temp_label[which(temp_label=="True")] <- "TRUE"	
	temp_label_factor <- factor(temp_label, order=TRUE, levels=c("FALSE", "TRUE"))
	data[label] <- temp_label_factor

	return (data)
}


add_average_for_data_frame <- function (data) {
    
    first_col_name <- names(data)[1]
    number_rows <- dim(data)[1]
    number_cols <- dim(data)[2]
    numeric_cols <- data[,2:number_cols]

	numeric_cols[,] <- sapply(numeric_cols[,], as.numeric)

    ave_values <- colMeans(numeric_cols)

    string_col <- as.character(data[,1])
    commit_or_project <- append(string_col, "Average")

    data[,1] <- NULL
    data[number_rows+1, ] <- ave_values

    data <- cbind(commit_or_project, data)
    names(data)[1] <- first_col_name
    return(data)
}


add_statis_rq1 <- function(data){
	temp_data <- data[1:(dim(data)[1]-1),] # remove average

	p_value_2 <- get_pvalue(temp_data, "ours_mrr", "rg_mrr",2)
	cliff_2 <- get_cliff (temp_data, "ours_mrr", "rg_mrr")
	p_value_4 <- get_pvalue(temp_data, "ours_mrr", "pmd_mrr",2)
	cliff_4 <- get_cliff (temp_data, "ours_mrr", "pmd_mrr")
	p_value_7 <- get_pvalue(temp_data, "ours_map", "rg_map",2)
	cliff_7 <- get_cliff (temp_data, "ours_map", "rg_map")
	p_value_9 <- get_pvalue(temp_data, "ours_map", "pmd_map",2)
	cliff_9 <- get_cliff (temp_data, "ours_map", "pmd_map")
	p_value_12 <- get_pvalue(temp_data, "ours_top1", "rg_top1",2)
	cliff_12 <- get_cliff (temp_data, "ours_top1", "rg_top1")
	p_value_13 <- get_pvalue(temp_data, "ours_top1", "pmd_top1",2)
	cliff_13 <- get_cliff (temp_data, "ours_top1", "pmd_top1")
	p_value_15 <- get_pvalue(temp_data, "ours_top5", "rg_top5",2)
	cliff_15 <- get_cliff (temp_data, "ours_top5", "rg_top5")
	p_value_16 <- get_pvalue(temp_data, "ours_top5", "pmd_top5",2)
	cliff_16 <- get_cliff (temp_data, "ours_top5", "pmd_top5")
	p_value_18 <- get_pvalue(temp_data, "ours_top10", "rg_top10",2)
	cliff_18 <- get_cliff (temp_data, "ours_top10", "rg_top10")
	p_value_19 <- get_pvalue(temp_data, "ours_top10", "pmd_top10",2)
	cliff_19 <- get_cliff (temp_data, "ours_top10", "pmd_top10")
    first_col_name <- names(data)[1]
    number_rows <- dim(data)[1]
    number_cols <- dim(data)[2]

    string_first_col <- as.character(data[,1])
    string_first_col <- append(string_first_col, "p_value")
    string_first_col <- append(string_first_col, "Cliff's delta")

    data[,1] <- NULL
    data[number_rows+1, ] <- c(p_value_2, " ", p_value_4, " ", " ", p_value_7, " ", p_value_9, " ", " ", p_value_12, 
    	p_value_13, " ", p_value_15, p_value_16, " ", p_value_18, p_value_19, " ")
    data[number_rows+2, ] <- c(cliff_2, " ", cliff_4, " ", " ", cliff_7, " ", cliff_9, " ", " ", cliff_12, 
    	cliff_13, " ", cliff_15, cliff_16, " ", cliff_18, cliff_19, " ")

    data <- cbind(string_first_col, data)
    names(data)[1] <- first_col_name
    return(data)
}

# add_statis_rq2 <- function(data){
# 	temp_data <- data[1:(dim(data)[1]-1),] # remove average
#     names <- names(temp_data)
# 	p_value_2 <- get_pvalue(temp_data, names[5], names[2])
# 	cliff_2 <- get_cliff (temp_data, names[5], names[2])
# 	p_value_3 <- get_pvalue(temp_data, names[5], names[3])
# 	cliff_3 <- get_cliff (temp_data, names[5], names[3])
# 	p_value_4 <- get_pvalue(temp_data, names[5], names[4])
# 	cliff_4 <- get_cliff (temp_data, names[5], names[4])
# 	p_value_6 <- get_pvalue(temp_data, names[9], names[6])
# 	cliff_6 <- get_cliff (temp_data, names[9], names[6])
# 	p_value_7 <- get_pvalue(temp_data, names[9], names[7])
# 	cliff_7 <- get_cliff (temp_data, names[9], names[7])
# 	p_value_8 <- get_pvalue(temp_data, names[9], names[8])
# 	cliff_8 <- get_cliff (temp_data, names[9], names[8])
# 	p_value_10 <- get_pvalue(temp_data, names[13], names[10])
# 	cliff_10 <- get_cliff (temp_data, names[13], names[10])
# 	p_value_11 <- get_pvalue(temp_data, names[13], names[11])
# 	cliff_11 <- get_cliff (temp_data, names[13], names[11])
# 	p_value_12 <- get_pvalue(temp_data, names[13], names[12])
# 	cliff_12 <- get_cliff (temp_data, names[13], names[12])
# 	p_value_14 <- get_pvalue(temp_data, names[17], names[14])
# 	cliff_14 <- get_cliff (temp_data, names[17], names[14])
# 	p_value_15 <- get_pvalue(temp_data, names[17], names[15])
# 	cliff_15 <- get_cliff (temp_data, names[17], names[15])
# 	p_value_16 <- get_pvalue(temp_data, names[17], names[16])
# 	cliff_16 <- get_cliff (temp_data, names[17], names[16])
#     first_col_name <- names(data)[1]
#     number_rows <- dim(data)[1]
#     number_cols <- dim(data)[2]

#     string_first_col <- as.character(data[,1])
#     string_first_col <- append(string_first_col, "p_value")
#     string_first_col <- append(string_first_col, "Cliff's delta")

#     data[,1] <- NULL
#     data[number_rows+1, ] <- c(p_value_2, p_value_3, p_value_4, " ", p_value_6, p_value_7, p_value_8, " ", p_value_10, 
#     	p_value_11, p_value_12, " ", p_value_14, p_value_15, p_value_16, " ")
#     data[number_rows+2, ] <- c(cliff_2, cliff_3, cliff_4, " ", cliff_6, cliff_7, cliff_8, " ", cliff_10, 
#     	cliff_11, cliff_12, " ", cliff_14, cliff_15, cliff_16, " ")

#     data <- cbind(string_first_col, data)
#     names(data)[1] <- first_col_name
#     return(data)
# }

add_statis_rq2 <- function(data){
	temp_data <- data[1:(dim(data)[1]-1),] # remove average
    names <- names(temp_data)
	p_value_2 <- get_pvalue(temp_data, names[3], names[2],1)
	cliff_2 <- get_cliff (temp_data, names[3], names[2])
	p_value_4 <- get_pvalue(temp_data, names[5], names[4],1)
	cliff_4 <- get_cliff (temp_data, names[5], names[4])
	p_value_6 <- get_pvalue(temp_data, names[7], names[6],1)
	cliff_6 <- get_cliff (temp_data, names[7], names[6])
	p_value_8 <- get_pvalue(temp_data, names[9], names[8],1)
	cliff_8 <- get_cliff (temp_data, names[9], names[8])
	p_value_10 <- get_pvalue(temp_data, names[11], names[10],1)
	cliff_10 <- get_cliff (temp_data, names[11], names[10])
	p_value_12 <- get_pvalue(temp_data, names[13], names[12],1)
	cliff_12 <- get_cliff (temp_data, names[13], names[12])
	p_value_14 <- get_pvalue(temp_data, names[15], names[14],1)
	cliff_14 <- get_cliff (temp_data, names[15], names[14])
	p_value_16 <- get_pvalue(temp_data, names[17], names[16],1)
	cliff_16 <- get_cliff (temp_data, names[17], names[16])
    first_col_name <- names(data)[1]
    number_rows <- dim(data)[1]
    number_cols <- dim(data)[2]

    string_first_col <- as.character(data[,1])
    string_first_col <- append(string_first_col, "p_value")
    string_first_col <- append(string_first_col, "Cliff's delta")

    data[,1] <- NULL
    data[number_rows+1, ] <- c(p_value_2, " ", p_value_4, " ", p_value_6, " ", p_value_8, " ", 
    	p_value_10, " ", p_value_12, " ", p_value_14, " ", p_value_16, " ")
    data[number_rows+2, ] <- c(cliff_2, " ", cliff_4, " ", cliff_6, " ", cliff_8, " ", 
    	cliff_10, " ", cliff_12, " ", cliff_14, " ", cliff_16, " ")

    data <- cbind(string_first_col, data)
    names(data)[1] <- first_col_name
    return(data)
}

add_statis_three_comparison <- function(data){
	temp_data <- data[1:(dim(data)[1]-1),] # remove average
    names <- names(temp_data)
	p_value_2 <- get_pvalue(temp_data, names[4], names[2],2)
	cliff_2 <- get_cliff (temp_data, names[4], names[2])
	p_value_3 <- get_pvalue(temp_data, names[4], names[3],2)
	cliff_3 <- get_cliff (temp_data, names[4], names[3])
	p_value_5 <- get_pvalue(temp_data, names[7], names[5],2)
	cliff_5 <- get_cliff (temp_data, names[7], names[5])
	p_value_6 <- get_pvalue(temp_data, names[7], names[6],2)
	cliff_6 <- get_cliff (temp_data, names[7], names[6])
	p_value_8 <- get_pvalue(temp_data, names[10], names[8],2)
	cliff_8 <- get_cliff (temp_data, names[10], names[8])
	p_value_9 <- get_pvalue(temp_data, names[10], names[9],2)
	cliff_9 <- get_cliff (temp_data, names[10], names[9])
	p_value_11 <- get_pvalue(temp_data, names[13], names[11],2)
	cliff_11 <- get_cliff (temp_data, names[13], names[11])
	p_value_12 <- get_pvalue(temp_data, names[13], names[12],2)
	cliff_12 <- get_cliff (temp_data, names[13], names[12])
	#p_value_16 <- get_pvalue(temp_data, names[17], names[16])
	#cliff_16 <- get_cliff (temp_data, names[17], names[16])
    first_col_name <- names(data)[1]
    number_rows <- dim(data)[1]
    number_cols <- dim(data)[2]

    string_first_col <- as.character(data[,1])
    string_first_col <- append(string_first_col, "p_value")
    string_first_col <- append(string_first_col, "Cliff's delta")

    data[,1] <- NULL
    data[number_rows+1, ] <- c(p_value_2, p_value_3, " ", p_value_5, p_value_6," ", p_value_8,p_value_9, " ", p_value_11, p_value_12," ")
    data[number_rows+2, ] <- c(cliff_2, cliff_3, " ", cliff_5, cliff_6," ", cliff_8,cliff_9, " ", cliff_11, cliff_12," ")

    data <- cbind(string_first_col, data)
    names(data)[1] <- first_col_name
    return(data)
}

add_statis_rq4 <- function(data){
	temp_data <- data[1:(dim(data)[1]-1),] # remove average
    names <- names(temp_data)
	p_value_2 <- get_pvalue(temp_data, names[3], names[2],1)
	cliff_2 <- get_cliff (temp_data, names[3], names[2])
	p_value_4 <- get_pvalue(temp_data, names[5], names[4],1)
	cliff_4 <- get_cliff (temp_data, names[5], names[4])
	p_value_6 <- get_pvalue(temp_data, names[7], names[6],1)
	cliff_6 <- get_cliff (temp_data, names[7], names[6])
	p_value_8 <- get_pvalue(temp_data, names[9], names[8],1)
	cliff_8 <- get_cliff (temp_data, names[9], names[8])

    first_col_name <- names(data)[1]
    number_rows <- dim(data)[1]
    number_cols <- dim(data)[2]

    string_first_col <- as.character(data[,1])
    string_first_col <- append(string_first_col, "p_value")
    string_first_col <- append(string_first_col, "Cliff's delta")

    data[,1] <- NULL
    data[number_rows+1, ] <- c(p_value_2, " " , p_value_4, " ", p_value_6, " ", p_value_8, " ")
    data[number_rows+2, ] <- c(cliff_2, " " , cliff_4, " ", cliff_6, " ", cliff_8, " ")

    data <- cbind(string_first_col, data)
    names(data)[1] <- first_col_name
    return(data)
}

get_pvalue <- function(data, col1, col2, num_baselines){  # bigger one is col1
	#num_baselines <- 2
	ret <- wilcox.test(data[col1][,1], data[col2][,1], alternative="g", var.equal=FALSE, paired = TRUE, conf.level=0.95)
	p_value <- ret["p.value"][[1]]  
	adjusted_pvalue <- p_value * num_baselines

	if (adjusted_pvalue <= 0.001){
      p_value_str <- "<0.001"
    }
    if (adjusted_pvalue <= 0.01 & adjusted_pvalue >0.001){
      p_value_str <- "<0.01"
    }
    if(adjusted_pvalue <= 0.05 & adjusted_pvalue > 0.01){
      p_value_str <- "<0.05"
    }
    if (adjusted_pvalue > 0.05){
      p_value_str <- ">0.05"
    }
    return(p_value_str)
}

get_cliff <- function(data, col1, col2){  # bigger one is col1
	ret <- cliff.delta(data[col1][,1], data[col2][,1])
	estimation <- ret["estimate"][[1]]
    estimation <- round(estimation, 2)
    if (ret["magnitude"] == 1){
      magnitude <- "(N)"
    }
    if (ret["magnitude"] == 2){
      magnitude <- "(S)"
    }
    if (ret["magnitude"] == 3){
      magnitude <- "(M)"
    }
    if (ret["magnitude"] == 4){
      magnitude <- "(L)"
    }

    cliff_str <- paste(c(estimation, magnitude), collapse="")
	return(cliff_str)
}